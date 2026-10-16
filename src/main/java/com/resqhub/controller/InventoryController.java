package com.resqhub.controller;

import com.resqhub.dto.InventoryItemDto;
import com.resqhub.dto.StockAdjustmentDto;
import com.resqhub.model.InventoryItem;
import com.resqhub.model.ItemCategory;
import com.resqhub.model.ItemUnit;
import com.resqhub.model.TransactionType;
import com.resqhub.model.User;
import com.resqhub.service.ResourceService;
import com.resqhub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final ResourceService resourceService;
    private final UserService userService;

    @Autowired
    public InventoryController(ResourceService resourceService, UserService userService) {
        this.resourceService = resourceService;
        this.userService = userService;
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return userService.findByUsername(auth.getName()).map(User::getId).orElse(null);
        }
        return null;
    }

    @GetMapping
    public String listInventory(@RequestParam(value = "category", required = false) String categoryFilter,
                                @RequestParam(value = "lowStockOnly", defaultValue = "false") boolean lowStockOnly,
                                Model model) {
        model.addAttribute("activeMenu", "inventory");

        List<InventoryItem> items;
        if (lowStockOnly) {
            items = resourceService.getLowStockItems();
        } else if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase("ALL")) {
            try {
                ItemCategory category = ItemCategory.valueOf(categoryFilter.toUpperCase());
                items = resourceService.getItemsByCategory(category);
            } catch (IllegalArgumentException e) {
                items = resourceService.getAllItems();
            }
        } else {
            items = resourceService.getAllItems();
        }

        model.addAttribute("items", items);
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("selectedCategory", categoryFilter != null ? categoryFilter : "ALL");
        model.addAttribute("lowStockOnly", lowStockOnly);

        model.addAttribute("totalItemCount", resourceService.getTotalItemCount());
        model.addAttribute("totalQuantity", resourceService.getTotalQuantity());
        model.addAttribute("lowStockCount", resourceService.getLowStockCount());

        return "resources/inventory-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String showCreateForm(Model model) {
        model.addAttribute("activeMenu", "inventory");
        if (!model.containsAttribute("itemDto")) {
            model.addAttribute("itemDto", new InventoryItemDto());
        }
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("units", ItemUnit.values());
        return "resources/item-form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String createItem(@Valid @ModelAttribute("itemDto") InventoryItemDto dto,
                             BindingResult bindingResult,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "inventory");
            model.addAttribute("categories", ItemCategory.values());
            model.addAttribute("units", ItemUnit.values());
            return "resources/item-form";
        }

        try {
            Long userId = getCurrentUserId(auth);
            InventoryItem created = resourceService.createItem(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Item '" + created.getItemName() + "' registered successfully into central inventory.");
            return "redirect:/inventory/" + created.getId();
        } catch (Exception e) {
            model.addAttribute("activeMenu", "inventory");
            model.addAttribute("errorMessage", "Failed to register item: " + e.getMessage());
            model.addAttribute("categories", ItemCategory.values());
            model.addAttribute("units", ItemUnit.values());
            return "resources/item-form";
        }
    }

    @GetMapping("/{id}")
    public String viewItemDetails(@PathVariable Long id, Model model) {
        model.addAttribute("activeMenu", "inventory");

        InventoryItem item = resourceService.getItemById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with id: " + id));

        model.addAttribute("item", item);
        model.addAttribute("transactions", resourceService.getItemTransactions(id));
        model.addAttribute("transactionTypes", TransactionType.values());

        if (!model.containsAttribute("adjustmentDto")) {
            StockAdjustmentDto adjustmentDto = new StockAdjustmentDto();
            adjustmentDto.setItemId(item.getId());
            model.addAttribute("adjustmentDto", adjustmentDto);
        }

        return "resources/item-details";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("activeMenu", "inventory");

        InventoryItem item = resourceService.getItemById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with id: " + id));

        if (!model.containsAttribute("itemDto")) {
            InventoryItemDto dto = new InventoryItemDto();
            dto.setId(item.getId());
            dto.setItemName(item.getItemName());
            dto.setCategory(item.getCategory());
            dto.setUnit(item.getUnit());
            dto.setDescription(item.getDescription());
            dto.setQuantityAvailable(item.getQuantityAvailable());
            dto.setMinimumStockLevel(item.getMinimumStockLevel());
            dto.setStorageLocation(item.getStorageLocation());
            model.addAttribute("itemDto", dto);
        }

        model.addAttribute("item", item);
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("units", ItemUnit.values());
        return "resources/item-form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String updateItem(@PathVariable Long id,
                             @Valid @ModelAttribute("itemDto") InventoryItemDto dto,
                             BindingResult bindingResult,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "inventory");
            model.addAttribute("categories", ItemCategory.values());
            model.addAttribute("units", ItemUnit.values());
            return "resources/item-form";
        }

        try {
            Long userId = getCurrentUserId(auth);
            InventoryItem updated = resourceService.updateItem(id, dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Item '" + updated.getItemName() + "' updated successfully.");
            return "redirect:/inventory/" + id;
        } catch (Exception e) {
            model.addAttribute("activeMenu", "inventory");
            model.addAttribute("errorMessage", "Failed to update item: " + e.getMessage());
            model.addAttribute("categories", ItemCategory.values());
            model.addAttribute("units", ItemUnit.values());
            return "resources/item-form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteItem(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            resourceService.deleteItem(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Item successfully deleted from inventory.");
            return "redirect:/inventory";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/inventory/" + id;
        }
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String adjustStock(@PathVariable Long id,
                              @Valid @ModelAttribute("adjustmentDto") StockAdjustmentDto dto,
                              BindingResult bindingResult,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        dto.setItemId(id);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.adjustmentDto", bindingResult);
            redirectAttributes.addFlashAttribute("adjustmentDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Stock adjustment validation failed. Quantity must be at least 1 and reason provided.");
            return "redirect:/inventory/" + id;
        }

        try {
            Long userId = getCurrentUserId(auth);
            resourceService.adjustStock(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Stock successfully updated with transaction record.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Adjustment failed: " + e.getMessage());
        }

        return "redirect:/inventory/" + id;
    }
}
