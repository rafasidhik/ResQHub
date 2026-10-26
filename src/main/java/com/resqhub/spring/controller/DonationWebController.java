package com.resqhub.spring.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.DonationController;
import com.resqhub.model.Donation;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;
import com.resqhub.model.Donor;
import com.resqhub.model.DonorType;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.service.SessionManager;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class DonationWebController extends BaseWebController {

    private final DonationController donationController = new DonationController();

    @GetMapping("/app/donations")
    public String listOrDetail(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null && (u.getRole() == RoleType.RESCUE_OFFICER || u.getRole() == RoleType.CITIZEN || u.getRole() == RoleType.VOLUNTEER)) {
            return "redirect:/app/dashboard";
        }
        model.addAttribute("donorTypes", WebUtil.options(DonorType.class));
        model.addAttribute("donationTypes", WebUtil.options(DonationType.class));
        model.addAttribute("donationStatuses", WebUtil.options(DonationStatus.class));

        List<Donor> donors = donationController.getAllDonors();
        if (id != null) {
            Donation d = donationController.getDonation(id);
            if (d == null) {
                model.addAttribute("errorTitle", "Donation not found");
                model.addAttribute("errorMessage", "Donation #" + id + " does not exist.");
                return "error";
            }
            var dists = donationController.getDistributions(id);
            Donor donor = donors.stream().filter(x -> x.getId().equals(d.getDonorId())).findFirst().orElse(null);
            model.addAttribute("d", d);
            model.addAttribute("donor", donor);
            model.addAttribute("distHeaders", DonationController.distributionTableHeaders());
            model.addAttribute("distRows", rows(dists, DonationController::toDistributionRow));
            model.addAttribute("pageTitle", "Donation #" + id);
            return "donation-detail";
        }

        List<Donation> donations = donationController.getAllDonations();
        Map<Long, String> donorNames = new HashMap<>();
        donors.forEach(x -> donorNames.put(x.getId(), x.getFullName()));

        model.addAttribute("donors", donors);
        model.addAttribute("donorHeaders", DonationController.donorTableHeaders());
        model.addAttribute("donorRows", rows(donors, DonationController::toDonorRow));
        model.addAttribute("donations", donations);
        model.addAttribute("donationHeaders", DonationController.donationTableHeaders());
        model.addAttribute("donationRows", rows(donations, DonationController::toDonationRow));
        model.addAttribute("donorOptions", opts(donors, Donor::getId, x -> x.getFullName() + " (" + x.getContactNumber() + ")"));
        model.addAttribute("donorNames", donorNames);
        model.addAttribute("sCash", donationController.totalCashDonated());
        model.addAttribute("sCount", donationController.countDonations());
        model.addAttribute("sDonors", donationController.countDonors());
        model.addAttribute("sDistributions", donationController.countDistributions());
        model.addAttribute("pageTitle", "Donation Management");
        return "donations";
    }

    @PostMapping("/app/donations")
    public String handleAction(@RequestParam("action") String action,
                               HttpServletRequest req,
                               RedirectAttributes ra) throws Exception {
        ActionResult r;
        String idParam = req.getParameter("id");
        String detailRedirect = idParam != null && !idParam.isBlank() ? "/app/donations?id=" + idParam : "/app/donations";

        switch (action) {
            case "addDonor" -> {
                r = donationController.registerDonor(
                        WebUtil.str(req, "fullName"),
                        WebUtil.str(req, "contactNumber"),
                        WebUtil.opt(req, "email"),
                        WebUtil.opt(req, "location"),
                        WebUtil.optEnum(req, "donorType", DonorType.class, "Donor type")
                );
                return handleResult(r, ra, "/app/donations");
            }
            case "updateDonor" -> {
                r = donationController.updateDonor(
                        WebUtil.reqLong(req, "id", "Donor id"),
                        WebUtil.str(req, "fullName"),
                        WebUtil.str(req, "contactNumber"),
                        WebUtil.opt(req, "email"),
                        WebUtil.opt(req, "location"),
                        WebUtil.optEnum(req, "donorType", DonorType.class, "Donor type")
                );
                return handleResult(r, ra, "/app/donations");
            }
            case "deleteDonor" -> {
                r = donationController.deleteDonor(WebUtil.reqLong(req, "id", "Donor id"));
                return handleResult(r, ra, "/app/donations");
            }
            case "addDonation" -> {
                r = donationController.recordDonation(
                        WebUtil.reqLong(req, "donorId", "Donor"),
                        WebUtil.reqEnum(req, "donationType", DonationType.class, "Donation type"),
                        WebUtil.str(req, "amount"),
                        WebUtil.opt(req, "materialName"),
                        qty(req),
                        WebUtil.opt(req, "description")
                );
                return handleResult(r, ra, "/app/donations");
            }
            case "updateDonation" -> r = donationController.updateDonation(
                    WebUtil.reqLong(req, "id", "Donation id"),
                    WebUtil.reqEnum(req, "donationType", DonationType.class, "Donation type"),
                    WebUtil.str(req, "amount"),
                    WebUtil.opt(req, "materialName"),
                    qty(req),
                    WebUtil.opt(req, "description")
            );
            case "status" -> r = donationController.updateDonationStatus(
                    WebUtil.reqLong(req, "id", "Donation id"),
                    WebUtil.reqEnum(req, "status", DonationStatus.class, "Status")
            );
            case "deleteDonation" -> {
                r = donationController.deleteDonation(WebUtil.reqLong(req, "id", "Donation id"));
                return handleResult(r, ra, "/app/donations");
            }
            case "distribute" -> r = donationController.recordDistribution(
                    WebUtil.reqLong(req, "id", "Donation id"),
                    WebUtil.str(req, "distributedTo"),
                    WebUtil.str(req, "quantity"),
                    WebUtil.opt(req, "description")
            );
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:" + detailRedirect;
            }
        }
        return handleResult(r, ra, detailRedirect);
    }

    private static String qty(HttpServletRequest req) {
        String q = WebUtil.str(req, "quantity");
        return q.isEmpty() ? "0" : q;
    }
}
