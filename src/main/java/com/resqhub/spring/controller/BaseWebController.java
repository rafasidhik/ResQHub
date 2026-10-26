package com.resqhub.spring.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.ActionResult;

public abstract class BaseWebController {

    public static <T> List<Object[]> rows(List<T> items, Function<T, Object[]> toRow) {
        List<Object[]> out = new ArrayList<>();
        if (items != null) {
            for (T t : items) {
                out.add(toRow.apply(t));
            }
        }
        return out;
    }

    public static <T> List<String[]> opts(List<T> items, Function<T, Object> valFn, Function<T, String> labelFn) {
        List<String[]> out = new ArrayList<>();
        if (items != null) {
            for (T t : items) {
                out.add(new String[] {String.valueOf(valFn.apply(t)), labelFn.apply(t)});
            }
        }
        return out;
    }

    protected String handleResult(ActionResult result, RedirectAttributes ra, String redirectSuccess, String redirectFail) {
        if (result.isSuccess()) {
            ra.addFlashAttribute("flashType", "success");
            ra.addFlashAttribute("flashMessage", result.getMessage());
            return "redirect:" + redirectSuccess;
        } else {
            ra.addFlashAttribute("flashType", "danger");
            ra.addFlashAttribute("flashMessage", result.getMessage());
            return "redirect:" + (redirectFail != null ? redirectFail : redirectSuccess);
        }
    }

    protected String handleResult(ActionResult result, RedirectAttributes ra, String redirectSuccess) {
        return handleResult(result, ra, redirectSuccess, redirectSuccess);
    }
}
