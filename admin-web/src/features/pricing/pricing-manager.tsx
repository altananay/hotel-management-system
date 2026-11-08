"use client";

import { RefreshCw, Star, Trash2 } from "lucide-react";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { AdminApiClientError } from "@/lib/api-client";
import { resolveAdminErrorMessage } from "@/lib/error-messages";
import { formatDateTime, formatMoney, formatRate, normalizeDecimalInput } from "./pricing-format";
import {
  useBasePricesQuery,
  useFxRatesQuery,
  useManualFxRatesQuery,
  useManualPricesQuery,
  useRatePlansQuery,
  useRoomTypesQuery,
  useSeasonPricesQuery,
  useSettingsQuery,
} from "./use-pricing-queries";
import { usePricingMutations } from "./use-pricing-mutations";
import type { PriceMatrix, RatePlan, Season } from "./schema";

type Amounts = Record<string, string>;

function keyOf(roomTypeId: string, guests: number) {
  return `${roomTypeId}:${guests}`;
}

function amountsFromMatrix(matrix: PriceMatrix | undefined): Amounts {
  if (!matrix) {
    return {};
  }
  const result: Amounts = {};
  for (const row of matrix.rows) {
    result[keyOf(row.roomTypeId, row.guests)] = row.amountTry;
  }
  return result;
}

function PriceMatrixSection({
  title,
  testId,
  matrix,
  amounts,
  onAmountChange,
  onSave,
  saving,
  isError,
  onRetry,
  onDelete,
}: {
  title: string;
  testId: string;
  matrix: PriceMatrix | undefined;
  amounts: Amounts;
  onAmountChange: (key: string, value: string) => void;
  onSave: () => void;
  saving: boolean;
  isError?: boolean;
  onRetry?: () => void;
  onDelete?: (rowId: string) => void;
}) {
  if (isError) {
    return (
      <Card className="mt-6" data-testid={testId}>
        <h2 className="text-lg font-semibold">{title}</h2>
        <div
          role="alert"
          className="mt-3 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
        >
          Fiyat matrisi yüklenemedi.
          <Button className="ml-3" size="sm" variant="secondary" onClick={onRetry}>
            Tekrar Dene
          </Button>
        </div>
      </Card>
    );
  }
  if (!matrix) {
    return (
      <Card className="mt-6">
        <h2 className="text-lg font-semibold">{title}</h2>
        <p className="mt-2 text-sm text-slate-600">Fiyat matrisi yükleniyor...</p>
      </Card>
    );
  }
  return (
    <Card className="mt-6" data-testid={testId}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">{title}</h2>
        <Button
          data-testid="admin-pricing-save"
          onClick={onSave}
          disabled={saving}
        >
          Fiyatları Kaydet
        </Button>
      </div>
      {matrix.roomTypes.length === 0 ? (
        <p className="mt-4 text-sm text-slate-600">Aktif oda tipi bulunmuyor.</p>
      ) : (
        <div className="mt-4 overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-left text-slate-600">
                <th className="py-2 pr-4">Oda Tipi</th>
                <th className="py-2 pr-4">Misafir</th>
                <th className="py-2 pr-4">TRY Gece Fiyatı</th>
                <th className="py-2 pr-4">Güncelleme</th>
                <th className="py-2 pr-4">Durum</th>
                <th className="py-2">İşlem</th>
              </tr>
            </thead>
            <tbody>
              {matrix.roomTypes.flatMap((roomType) =>
                Array.from({ length: Math.max(roomType.maxGuests, 1) }, (_, index) => {
                  const guests = index + 1;
                  const key = keyOf(roomType.id, guests);
                  const storedRow = matrix.rows.find(
                    (row) =>
                      row.roomTypeId === roomType.id && row.guests === guests,
                  );
                  return (
                    <tr
                      key={key}
                      data-testid="admin-pricing-price-row"
                      className="border-b border-slate-100"
                    >
                      <td className="py-2 pr-4">{roomType.nameTr}</td>
                      <td className="py-2 pr-4">{guests}</td>
                      <td className="py-2 pr-4">
                        <input
                          aria-label={`${roomType.nameTr} ${guests} misafir fiyatı`}
                          className="w-36 rounded-md border border-slate-300 px-2 py-1"
                          inputMode="decimal"
                          value={amounts[key] ?? ""}
                          disabled={!roomType.active}
                          onChange={(event) =>
                            onAmountChange(key, event.target.value)
                          }
                        />
                      </td>
                      <td className="py-2 pr-4 text-xs text-slate-500">
                        {formatDateTime(storedRow?.updatedAt ?? "")}
                      </td>
                      <td className="py-2 pr-4">
                        {roomType.active ? (
                          <span className="text-emerald-700">Aktif</span>
                        ) : (
                          <span className="text-slate-500">Pasif / salt okunur</span>
                        )}
                      </td>
                      <td className="py-2">
                        {storedRow && roomType.active && onDelete && (
                          <Button
                            size="sm"
                            variant="secondary"
                            data-testid="admin-pricing-price-delete"
                            onClick={() => onDelete(storedRow.id)}
                          >
                            Sil
                          </Button>
                        )}
                      </td>
                    </tr>
                  );
                }),
              )}
            </tbody>
          </table>
        </div>
      )}
      {matrix.missing.length > 0 && (
        <div
          role="status"
          className="mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900"
        >
          Eksik fiyatlar: {matrix.missing.length} oda tipi/misafir kombinasyonu.
          Varsayılan plan için tüm aktif kombinasyonlar doldurulmalıdır.
        </div>
      )}
    </Card>
  );
}

export function PricingManager() {
  const plansQuery = useRatePlansQuery();
  const settingsQuery = useSettingsQuery();
  const fxQuery = useFxRatesQuery();
  const roomTypesQuery = useRoomTypesQuery();
  const mutations = usePricingMutations();

  const [selectedPlanId, setSelectedPlanId] = useState<string | null>(null);
  const [selectedSeasonId, setSelectedSeasonId] = useState<string | null>(null);
  const [baseAmounts, setBaseAmounts] = useState<Amounts>({});
  const [seasonAmounts, setSeasonAmounts] = useState<Amounts>({});
  const [actionError, setActionError] = useState<string | null>(null);

  const plans = useMemo(() => plansQuery.data ?? [], [plansQuery.data]);
  const roomTypes = useMemo(
    () => roomTypesQuery.data ?? [],
    [roomTypesQuery.data],
  );
  const basePricesQuery = useBasePricesQuery(selectedPlanId);
  const manualPricesQuery = useManualPricesQuery(selectedPlanId);
  const seasonPricesQuery = useSeasonPricesQuery(selectedSeasonId);
  const manualFxQuery = useManualFxRatesQuery();

  const [planForm, setPlanForm] = useState({
    code: "",
    nameTr: "",
    nameEn: "",
  });
  const [planEdit, setPlanEdit] = useState({
    id: "",
    nameTr: "",
    nameEn: "",
  });
  const [seasonForm, setSeasonForm] = useState({
    id: "",
    code: "",
    nameTr: "",
    nameEn: "",
    startDate: "",
    endDate: "",
  });
  const [manualPriceForm, setManualPriceForm] = useState({
    roomTypeId: "",
    guests: "1",
    startDate: "",
    endDate: "",
    amountTry: "",
  });
  const [manualFxForm, setManualFxForm] = useState({
    currency: "EUR",
    rateDate: "",
    rate: "",
  });
  const [bufferPercent, setBufferPercent] = useState("2.00");
  const [roundingStep, setRoundingStep] = useState("5.00");

  const selectedPlan = useMemo(
    () => plans.find((plan) => plan.id === selectedPlanId) ?? null,
    [plans, selectedPlanId],
  );
  const seasons = useMemo(
    () => selectedPlan?.seasons ?? [],
    [selectedPlan],
  );

  useEffect(() => {
    if (plans.length === 0) {
      setSelectedPlanId(null);
      return;
    }
    const current = plans.find((plan) => plan.id === selectedPlanId);
    if (!current) {
      const next = plans.find((plan) => plan.defaultPlan) ?? plans[0];
      setSelectedPlanId(next.id);
    }
  }, [plans, selectedPlanId]);

  useEffect(() => {
    if (seasons.length === 0) {
      setSelectedSeasonId(null);
      return;
    }
    if (!seasons.some((season) => season.id === selectedSeasonId)) {
      setSelectedSeasonId(seasons[0].id);
    }
  }, [seasons, selectedSeasonId]);

  useEffect(() => {
    setBaseAmounts(amountsFromMatrix(basePricesQuery.data));
  }, [basePricesQuery.data]);

  useEffect(() => {
    setSeasonAmounts(amountsFromMatrix(seasonPricesQuery.data));
  }, [seasonPricesQuery.data]);

  useEffect(() => {
    if (settingsQuery.data) {
      setBufferPercent(settingsQuery.data.bufferPercent);
      setRoundingStep(settingsQuery.data.roundingStep);
    }
  }, [settingsQuery.data]);

  const run = async (action: () => Promise<unknown>) => {
    setActionError(null);
    try {
      await action();
    } catch (error) {
      const code =
        error instanceof AdminApiClientError ? error.code : undefined;
      const missing =
        error instanceof AdminApiClientError ? error.missing : undefined;
      if (
        code === "pricing.default_rate_plan_incomplete" &&
        missing &&
        missing.length > 0
      ) {
        const names = new Map(roomTypes.map((room) => [room.id, room.nameTr]));
        setActionError(
          `Varsayılan plan için eksik fiyatlar: ${missing
            .map(
              (item) =>
                `${names.get(item.roomTypeId) ?? item.roomTypeId} / ${item.guests} misafir`,
            )
            .join(", ")}`,
        );
        return;
      }
      setActionError(resolveAdminErrorMessage(code));
    }
  };

  const refreshAll = () => {
    void plansQuery.refetch();
    void settingsQuery.refetch();
    void fxQuery.refetch();
    void roomTypesQuery.refetch();
    void basePricesQuery.refetch();
    void manualPricesQuery.refetch();
    void seasonPricesQuery.refetch();
    void manualFxQuery.refetch();
  };

  if (
    plansQuery.isLoading ||
    settingsQuery.isLoading ||
    fxQuery.isLoading ||
    roomTypesQuery.isLoading
  ) {
    return (
      <div data-testid="admin-pricing-page" className="mx-auto max-w-6xl px-6 py-12">
        <div data-testid="admin-pricing-loading" className="space-y-4">
          <Skeleton className="h-10 w-64" />
          <Skeleton className="h-40 w-full" />
          <Skeleton className="h-40 w-full" />
        </div>
      </div>
    );
  }

  if (
    plansQuery.isError ||
    settingsQuery.isError ||
    fxQuery.isError ||
    roomTypesQuery.isError
  ) {
    return (
      <div data-testid="admin-pricing-page" className="mx-auto max-w-6xl px-6 py-12">
        <div
          role="alert"
          data-testid="admin-pricing-error"
          className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800"
        >
          Fiyat yönetimi yüklenemedi. Lütfen tekrar deneyin.
        </div>
        <Button
          className="mt-4"
          data-testid="admin-pricing-retry"
          onClick={refreshAll}
        >
          <RefreshCw className="h-4 w-4" aria-hidden="true" />
          Tekrar Dene
        </Button>
      </div>
    );
  }

  const latest = fxQuery.data?.latest ?? [];
  const stale = fxQuery.data?.stale ?? false;

  const submitPlan = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await mutations.createRatePlan.mutateAsync({
        code: planForm.code,
        nameTr: planForm.nameTr,
        nameEn: planForm.nameEn,
        active: true,
      });
      setPlanForm({ code: "", nameTr: "", nameEn: "" });
    });
  };

  const submitPlanEdit = (event: FormEvent) => {
    event.preventDefault();
    if (!planEdit.id) {
      return;
    }
    void run(async () => {
      await mutations.updateRatePlan.mutateAsync({
        id: planEdit.id,
        nameTr: planEdit.nameTr,
        nameEn: planEdit.nameEn,
      });
      setPlanEdit({ id: "", nameTr: "", nameEn: "" });
    });
  };

  const submitSeason = (event: FormEvent) => {
    event.preventDefault();
    if (!selectedPlanId) {
      return;
    }
    void run(async () => {
      if (seasonForm.id) {
        await mutations.updateSeason.mutateAsync({
          id: seasonForm.id,
          nameTr: seasonForm.nameTr,
          nameEn: seasonForm.nameEn,
          startDate: seasonForm.startDate,
          endDate: seasonForm.endDate,
        });
      } else {
        await mutations.createSeason.mutateAsync({
          ratePlanId: selectedPlanId,
          code: seasonForm.code,
          nameTr: seasonForm.nameTr,
          nameEn: seasonForm.nameEn,
          startDate: seasonForm.startDate,
          endDate: seasonForm.endDate,
        });
      }
      setSeasonForm({
        id: "",
        code: "",
        nameTr: "",
        nameEn: "",
        startDate: "",
        endDate: "",
      });
    });
  };

  const submitManualPrice = (event: FormEvent) => {
    event.preventDefault();
    if (!selectedPlanId) {
      return;
    }
    void run(async () => {
      await mutations.createManualPrice.mutateAsync({
        ratePlanId: selectedPlanId,
        roomTypeId: manualPriceForm.roomTypeId,
        guests: Number(manualPriceForm.guests),
        startDate: manualPriceForm.startDate,
        endDate: manualPriceForm.endDate,
        amountTry: normalizeDecimalInput(manualPriceForm.amountTry),
      });
      setManualPriceForm({
        roomTypeId: "",
        guests: "1",
        startDate: "",
        endDate: "",
        amountTry: "",
      });
    });
  };

  const submitManualFx = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await mutations.createManualFxRate.mutateAsync({
        currency: manualFxForm.currency,
        rateDate: manualFxForm.rateDate,
        rate: normalizeDecimalInput(manualFxForm.rate),
      });
      setManualFxForm({ currency: "EUR", rateDate: "", rate: "" });
    });
  };

  const submitSettings = (event: FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await mutations.updateSettings.mutateAsync({
        bufferPercent: normalizeDecimalInput(bufferPercent),
        roundingStep: normalizeDecimalInput(roundingStep),
      });
    });
  };

  const saveMatrix = (
    matrix: PriceMatrix | undefined,
    amounts: Amounts,
    save: (rows: Array<{ roomTypeId: string; guests: number; amountTry: string }>) => Promise<unknown>,
  ) => {
    if (!matrix) {
      return;
    }
    const rows = matrix.roomTypes.flatMap((roomType) =>
      Array.from({ length: roomType.maxGuests }, (_, index) => {
        const guests = index + 1;
        const value = amounts[keyOf(roomType.id, guests)];
        if (!value || value.trim() === "" || !roomType.active) {
          return [];
        }
        return [
          {
            roomTypeId: roomType.id,
            guests,
            amountTry: normalizeDecimalInput(value),
          },
        ];
      }).flat(),
    );
    void run(() => save(rows));
  };

  const planFormView = (
    <form className="mt-4 grid gap-3 md:grid-cols-4" onSubmit={submitPlan}>
      <input
        aria-label="Plan kodu"
        className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        placeholder="Kod (ör. STANDARD)"
        value={planForm.code}
        onChange={(event) =>
          setPlanForm((current) => ({ ...current, code: event.target.value }))
        }
      />
      <input
        aria-label="Plan TR adı"
        className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        placeholder="TR ad"
        value={planForm.nameTr}
        onChange={(event) =>
          setPlanForm((current) => ({ ...current, nameTr: event.target.value }))
        }
      />
      <input
        aria-label="Plan EN adı"
        className="rounded-md border border-slate-300 px-3 py-2 text-sm"
        placeholder="EN ad"
        value={planForm.nameEn}
        onChange={(event) =>
          setPlanForm((current) => ({ ...current, nameEn: event.target.value }))
        }
      />
      <Button type="submit" data-testid="admin-pricing-save">
        Plan Ekle
      </Button>
    </form>
  );

  return (
    <main data-testid="admin-pricing-page" className="mx-auto max-w-6xl px-6 py-12">
      {plans.length > 0 && (
        <div data-testid="admin-pricing-filled" className="hidden" />
      )}
      <header className="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 pb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Fiyat Yönetimi</h1>
          <p className="mt-1 text-sm text-slate-600">
            Tarife planları, sezonlar, TRY baz fiyatları, manuel fiyat/kur ve
            buffer/yuvarlama ayarları.
          </p>
        </div>
        <Button onClick={refreshAll}>Yenile</Button>
      </header>

      <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
        Değişiklikler yalnız yeni fiyat tekliflerini etkiler; mevcut fiyat
        anlık görüntüleri değişmez.
      </div>

      {actionError && (
        <div
          role="alert"
          data-testid="admin-pricing-action-error"
          className="mt-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
        >
          {actionError}
        </div>
      )}

      <Card className="mt-6" data-testid="admin-pricing-fx-summary">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">Güncel Kur</h2>
          {stale && (
            <span
              data-testid="admin-pricing-fx-warning"
              className="rounded-md border border-amber-200 bg-amber-50 px-3 py-1 text-sm text-amber-900"
            >
              TCMB kuru bayat: {fxQuery.data?.staleCurrencies.join(", ")}
            </span>
          )}
        </div>
        <div className="mt-3 overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-left text-slate-600">
                <th className="py-2 pr-4">Para Birimi</th>
                <th className="py-2 pr-4">Kaynak</th>
                <th className="py-2 pr-4">Kur Tarihi</th>
                <th className="py-2 pr-4">Kur</th>
                <th className="py-2">Son Güncelleme</th>
              </tr>
            </thead>
            <tbody>
              {latest.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-3 text-slate-600">
                    Henüz kur kaydı yok. EUR/USD teklifi için manuel kur girin.
                  </td>
                </tr>
              ) : (
                latest.map((rate) => (
                  <tr key={rate.id} className="border-b border-slate-100">
                    <td className="py-2 pr-4">{rate.currency}</td>
                    <td className="py-2 pr-4">{rate.source}</td>
                    <td className="py-2 pr-4">{rate.rateDate}</td>
                    <td className="py-2 pr-4">{formatRate(rate.rate)}</td>
                    <td className="py-2">{formatDateTime(rate.updatedAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      <Card className="mt-6" data-testid="admin-pricing-settings">
        <h2 className="text-lg font-semibold">Buffer ve Yuvarlama</h2>
        <form className="mt-3 flex flex-wrap items-end gap-3" onSubmit={submitSettings}>
          <label className="text-sm">
            <span className="block text-slate-600">Buffer %</span>
            <input
              aria-label="Buffer yüzdesi"
              className="mt-1 w-32 rounded-md border border-slate-300 px-2 py-1"
              value={bufferPercent}
              onChange={(event) => setBufferPercent(event.target.value)}
            />
          </label>
          <label className="text-sm">
            <span className="block text-slate-600">Yuvarlama adımı</span>
            <input
              aria-label="Yuvarlama adımı"
              className="mt-1 w-32 rounded-md border border-slate-300 px-2 py-1"
              value={roundingStep}
              onChange={(event) => setRoundingStep(event.target.value)}
            />
          </label>
          <Button type="submit" data-testid="admin-pricing-save">
            Ayarları Kaydet
          </Button>
          <span className="text-xs text-slate-500">
            Son güncelleme: {formatDateTime(settingsQuery.data?.updatedAt ?? "")}
          </span>
        </form>
      </Card>

      <Card className="mt-6" data-testid="admin-pricing-rate-plans">
        <h2 className="text-lg font-semibold">Tarife Planları</h2>
        {plans.length === 0 ? (
          <div data-testid="admin-pricing-empty" className="mt-4 text-sm text-slate-600">
            Henüz tarife planı yok. İlk planı oluşturun.
          </div>
        ) : (
          <div className="mt-4 space-y-2">
            {plans.map((plan: RatePlan) => (
              <div
                key={plan.id}
                data-testid="admin-pricing-rate-plan-row"
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-200 p-3"
              >
                <div>
                  <div className="font-medium">
                    {plan.code} · {plan.nameTr} / {plan.nameEn}
                  </div>
                  <div className="text-xs text-slate-500">
                    {plan.active ? "Aktif" : "Pasif"} · Güncelleme:{" "}
                    {formatDateTime(plan.updatedAt)}
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  {plan.defaultPlan && (
                    <span
                      data-testid="admin-pricing-rate-plan-default"
                      className="rounded-md bg-emerald-50 px-2 py-1 text-xs text-emerald-800"
                    >
                      Varsayılan
                    </span>
                  )}
                  {!plan.defaultPlan && (
                    <Button
                      size="sm"
                      onClick={() =>
                        void run(() => mutations.setDefault.mutateAsync(plan.id))
                      }
                    >
                      <Star className="h-3 w-3" aria-hidden="true" />
                      Varsayılan Yap
                    </Button>
                  )}
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() =>
                      void run(() =>
                        mutations.updateRatePlan.mutateAsync({
                          id: plan.id,
                          active: !plan.active,
                        }),
                      )
                    }
                  >
                    {plan.active ? "Pasifleştir" : "Aktifleştir"}
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() =>
                      setPlanEdit({
                        id: plan.id,
                        nameTr: plan.nameTr,
                        nameEn: plan.nameEn,
                      })
                    }
                  >
                    Düzenle
                  </Button>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => setSelectedPlanId(plan.id)}
                  >
                    Seç
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
        {planFormView}
        {planEdit.id && (
          <form
            className="mt-4 grid gap-3 md:grid-cols-3"
            onSubmit={submitPlanEdit}
          >
            <input
              aria-label="Plan düzenleme TR adı"
              className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              value={planEdit.nameTr}
              onChange={(event) =>
                setPlanEdit((current) => ({
                  ...current,
                  nameTr: event.target.value,
                }))
              }
            />
            <input
              aria-label="Plan düzenleme EN adı"
              className="rounded-md border border-slate-300 px-3 py-2 text-sm"
              value={planEdit.nameEn}
              onChange={(event) =>
                setPlanEdit((current) => ({
                  ...current,
                  nameEn: event.target.value,
                }))
              }
            />
            <div className="flex gap-2">
              <Button type="submit" data-testid="admin-pricing-save">
                Planı Güncelle
              </Button>
              <Button
                variant="secondary"
                onClick={() => setPlanEdit({ id: "", nameTr: "", nameEn: "" })}
              >
                Vazgeç
              </Button>
            </div>
          </form>
        )}
      </Card>

      {plans.length > 0 && selectedPlan && (
        <>
          <Card className="mt-6" data-testid="admin-pricing-seasons">
            <h2 className="text-lg font-semibold">
              Sezonlar · {selectedPlan.code}
            </h2>
            {seasons.length === 0 ? (
              <p className="mt-3 text-sm text-slate-600">Henüz sezon yok.</p>
            ) : (
              <div className="mt-3 space-y-2">
                {seasons.map((season: Season) => (
                  <div
                    key={season.id}
                    data-testid="admin-pricing-season-row"
                    className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-200 p-3"
                  >
                    <div>
                      <div className="font-medium">
                        {season.code} · {season.nameTr} / {season.nameEn}
                      </div>
                      <div className="text-xs text-slate-500">
                        {season.startDate} → {season.endDate} · Güncelleme:{" "}
                        {formatDateTime(season.updatedAt)}
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() =>
                          setSeasonForm({
                            id: season.id,
                            code: season.code,
                            nameTr: season.nameTr,
                            nameEn: season.nameEn,
                            startDate: season.startDate,
                            endDate: season.endDate,
                          })
                        }
                      >
                        Düzenle
                      </Button>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() =>
                          void run(() =>
                            mutations.deleteSeason.mutateAsync(season.id),
                          )
                        }
                      >
                        <Trash2 className="h-3 w-3" aria-hidden="true" />
                        Sil
                      </Button>
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => setSelectedSeasonId(season.id)}
                      >
                        Fiyatları Aç
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
            <form className="mt-4 grid gap-3 md:grid-cols-6" onSubmit={submitSeason}>
              <input
                aria-label="Sezon kodu"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                placeholder="Kod"
                value={seasonForm.code}
                disabled={Boolean(seasonForm.id)}
                onChange={(event) =>
                  setSeasonForm((current) => ({
                    ...current,
                    code: event.target.value,
                  }))
                }
              />
              <input
                aria-label="Sezon TR adı"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                placeholder="TR ad"
                value={seasonForm.nameTr}
                onChange={(event) =>
                  setSeasonForm((current) => ({
                    ...current,
                    nameTr: event.target.value,
                  }))
                }
              />
              <input
                aria-label="Sezon EN adı"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                placeholder="EN ad"
                value={seasonForm.nameEn}
                onChange={(event) =>
                  setSeasonForm((current) => ({
                    ...current,
                    nameEn: event.target.value,
                  }))
                }
              />
              <input
                aria-label="Sezon başlangıç"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                type="date"
                value={seasonForm.startDate}
                onChange={(event) =>
                  setSeasonForm((current) => ({
                    ...current,
                    startDate: event.target.value,
                  }))
                }
              />
              <input
                aria-label="Sezon bitiş"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                type="date"
                value={seasonForm.endDate}
                onChange={(event) =>
                  setSeasonForm((current) => ({
                    ...current,
                    endDate: event.target.value,
                  }))
                }
              />
              <Button type="submit" data-testid="admin-pricing-save">
                {seasonForm.id ? "Sezonu Güncelle" : "Sezon Ekle"}
              </Button>
            </form>
          </Card>

          <PriceMatrixSection
            title="Baz Fiyat Matrisi"
            testId="admin-pricing-prices"
            matrix={basePricesQuery.data}
            amounts={baseAmounts}
            onAmountChange={(key, value) =>
              setBaseAmounts((current) => ({ ...current, [key]: value }))
            }
            onSave={() =>
              saveMatrix(basePricesQuery.data, baseAmounts, (rows) =>
                mutations.saveBasePrices.mutateAsync({
                  ratePlanId: selectedPlan.id,
                  rows,
                }),
              )
            }
            saving={mutations.saveBasePrices.isPending}
            isError={basePricesQuery.isError}
            onRetry={() => void basePricesQuery.refetch()}
            onDelete={(rowId) =>
              void run(() =>
                mutations.deleteBasePrice.mutateAsync({
                  ratePlanId: selectedPlan.id,
                  priceRowId: rowId,
                }),
              )
            }
          />

          {selectedSeasonId && (
            <PriceMatrixSection
              title="Sezon Fiyat Matrisi"
              testId="admin-pricing-season-prices"
              matrix={seasonPricesQuery.data}
              amounts={seasonAmounts}
              onAmountChange={(key, value) =>
                setSeasonAmounts((current) => ({ ...current, [key]: value }))
              }
              onSave={() =>
                saveMatrix(seasonPricesQuery.data, seasonAmounts, (rows) =>
                  mutations.saveSeasonPrices.mutateAsync({
                    seasonId: selectedSeasonId,
                    rows,
                  }),
                )
              }
              saving={mutations.saveSeasonPrices.isPending}
              isError={seasonPricesQuery.isError}
              onRetry={() => void seasonPricesQuery.refetch()}
              onDelete={(rowId) =>
                void run(() =>
                  mutations.deleteSeasonPrice.mutateAsync({
                    seasonId: selectedSeasonId,
                    priceRowId: rowId,
                  }),
                )
              }
            />
          )}

          <Card className="mt-6" data-testid="admin-pricing-manual-prices">
            <h2 className="text-lg font-semibold">Manuel Fiyatlar</h2>
            {manualPricesQuery.isError && (
              <div
                role="alert"
                className="mt-3 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
              >
                Manuel fiyatlar yüklenemedi.
                <Button
                  className="ml-3"
                  size="sm"
                  variant="secondary"
                  onClick={() => void manualPricesQuery.refetch()}
                >
                  Tekrar Dene
                </Button>
              </div>
            )}
            <div className="mt-3 overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-slate-600">
                    <th className="py-2 pr-3">Oda Tipi</th>
                    <th className="py-2 pr-3">Misafir</th>
                    <th className="py-2 pr-3">Dönem</th>
                    <th className="py-2 pr-3">TRY</th>
                    <th className="py-2 pr-3">Güncelleme</th>
                    <th className="py-2">İşlem</th>
                  </tr>
                </thead>
                <tbody>
                  {(manualPricesQuery.data ?? []).length === 0 &&
                  !manualPricesQuery.isError ? (
                    <tr>
                      <td colSpan={6} className="py-3 text-slate-600">
                        Manuel fiyat yok.
                      </td>
                    </tr>
                  ) : (
                    (manualPricesQuery.data ?? []).map((price) => (
                      <tr key={price.id} className="border-b border-slate-100">
                        <td className="py-2 pr-3">
                          {price.roomTypeNameTr ?? price.roomTypeId}
                        </td>
                        <td className="py-2 pr-3">{price.guests}</td>
                        <td className="py-2 pr-3">
                          {price.startDate} → {price.endDate}
                        </td>
                        <td className="py-2 pr-3">
                          {formatMoney(price.amountTry, "TRY")}
                        </td>
                        <td className="py-2 pr-3">
                          {formatDateTime(price.updatedAt)}
                        </td>
                        <td className="py-2">
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() =>
                              void run(() =>
                                mutations.deleteManualPrice.mutateAsync(price.id),
                              )
                            }
                          >
                            Sil
                          </Button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <form className="mt-4 grid gap-3 md:grid-cols-6" onSubmit={submitManualPrice}>
              <select
                aria-label="Manuel fiyat oda tipi"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                value={manualPriceForm.roomTypeId}
                onChange={(event) =>
                  setManualPriceForm((current) => ({
                    ...current,
                    roomTypeId: event.target.value,
                  }))
                }
              >
                <option value="">Oda tipi seçin</option>
                {roomTypes.map((roomType) => (
                  <option key={roomType.id} value={roomType.id}>
                    {roomType.nameTr} (max {roomType.maxGuests})
                  </option>
                ))}
              </select>
              <input
                aria-label="Manuel fiyat misafir sayısı"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                type="number"
                min={1}
                value={manualPriceForm.guests}
                onChange={(event) =>
                  setManualPriceForm((current) => ({
                    ...current,
                    guests: event.target.value,
                  }))
                }
              />
              <input
                aria-label="Manuel fiyat başlangıç"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                type="date"
                value={manualPriceForm.startDate}
                onChange={(event) =>
                  setManualPriceForm((current) => ({
                    ...current,
                    startDate: event.target.value,
                  }))
                }
              />
              <input
                aria-label="Manuel fiyat bitiş"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                type="date"
                value={manualPriceForm.endDate}
                onChange={(event) =>
                  setManualPriceForm((current) => ({
                    ...current,
                    endDate: event.target.value,
                  }))
                }
              />
              <input
                aria-label="Manuel fiyat tutarı"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                placeholder="TRY"
                value={manualPriceForm.amountTry}
                onChange={(event) =>
                  setManualPriceForm((current) => ({
                    ...current,
                    amountTry: event.target.value,
                  }))
                }
              />
              <Button type="submit" data-testid="admin-pricing-save">
                Manuel Fiyat Ekle
              </Button>
            </form>
          </Card>

        </>
      )}

      <Card className="mt-6" data-testid="admin-pricing-manual-fx">
        <h2 className="text-lg font-semibold">Manuel Kur</h2>
        {manualFxQuery.isError && (
          <div
            role="alert"
            className="mt-3 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800"
          >
            Manuel kur listesi yüklenemedi.
            <Button
              className="ml-3"
              size="sm"
              variant="secondary"
              onClick={() => void manualFxQuery.refetch()}
            >
              Tekrar Dene
            </Button>
          </div>
        )}
            <div className="mt-3 overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-slate-600">
                    <th className="py-2 pr-3">Para Birimi</th>
                    <th className="py-2 pr-3">Tarih</th>
                    <th className="py-2 pr-3">Kur</th>
                    <th className="py-2">İşlem</th>
                  </tr>
                </thead>
                <tbody>
                  {(manualFxQuery.data?.items ?? []).length === 0 &&
                  !manualFxQuery.isError ? (
                    <tr>
                      <td colSpan={4} className="py-3 text-slate-600">
                        Manuel kur yok.
                      </td>
                    </tr>
                  ) : (
                    (manualFxQuery.data?.items ?? []).map((rate) => (
                      <tr key={rate.id} className="border-b border-slate-100">
                        <td className="py-2 pr-3">{rate.currency}</td>
                        <td className="py-2 pr-3">{rate.rateDate}</td>
                        <td className="py-2 pr-3">{formatRate(rate.rate)}</td>
                        <td className="py-2">
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() =>
                              void run(() =>
                                mutations.deleteManualFxRate.mutateAsync(rate.id),
                              )
                            }
                          >
                            Sil
                          </Button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
            <form className="mt-4 grid gap-3 md:grid-cols-4" onSubmit={submitManualFx}>
              <select
                aria-label="Manuel kur para birimi"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                value={manualFxForm.currency}
                onChange={(event) =>
                  setManualFxForm((current) => ({
                    ...current,
                    currency: event.target.value,
                  }))
                }
              >
                <option value="EUR">EUR</option>
                <option value="USD">USD</option>
              </select>
              <input
                aria-label="Manuel kur tarihi"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                type="date"
                value={manualFxForm.rateDate}
                onChange={(event) =>
                  setManualFxForm((current) => ({
                    ...current,
                    rateDate: event.target.value,
                  }))
                }
              />
              <input
                aria-label="Manuel kur değeri"
                className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                placeholder="Kur"
                value={manualFxForm.rate}
                onChange={(event) =>
                  setManualFxForm((current) => ({
                    ...current,
                    rate: event.target.value,
                  }))
                }
              />
              <Button type="submit" data-testid="admin-pricing-save">
                Manuel Kur Ekle
              </Button>
            </form>
          </Card>

          <Card className="mt-6" data-testid="admin-pricing-fx-history">
            <h2 className="text-lg font-semibold">Kur Geçmişi</h2>
            <div className="mt-3 overflow-x-auto">
              <table className="min-w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-slate-600">
                    <th className="py-2 pr-3">Tarih</th>
                    <th className="py-2 pr-3">Para Birimi</th>
                    <th className="py-2 pr-3">Kaynak</th>
                    <th className="py-2 pr-3">Kur</th>
                    <th className="py-2">Çekim</th>
                  </tr>
                </thead>
                <tbody>
                  {(fxQuery.data?.items ?? []).map((rate) => (
                    <tr key={rate.id} className="border-b border-slate-100">
                      <td className="py-2 pr-3">{rate.rateDate}</td>
                      <td className="py-2 pr-3">{rate.currency}</td>
                      <td className="py-2 pr-3">{rate.source}</td>
                      <td className="py-2 pr-3">{formatRate(rate.rate)}</td>
                      <td className="py-2">{formatDateTime(rate.capturedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
    </main>
  );
}
