package com.hotel.scaffold;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary scaffold read endpoint for Spec 0001 ({@code specs/done/0001-project-architecture.md},
 * REQ-11 / AC-11) to verify the three mandatory UI states (loading, empty, error) in {@code
 * admin-web} via TanStack Query and Zod validation.
 *
 * <p>This temporary scaffold controller will be removed in the first real business feature spec.
 */
@RestController
@RequestMapping("/admin-api/system/sample-items")
public class AdminSampleItemsController {

    // TODO(scaffold-0001): Remove temporary scaffold endpoint in the first real business feature spec.
    @GetMapping
    public SampleItemsResponse getSampleItems() {
        return new SampleItemsResponse(List.of());
    }

    public record SampleItemDto(String id, String name) {}

    public record SampleItemsResponse(List<SampleItemDto> items) {}
}
