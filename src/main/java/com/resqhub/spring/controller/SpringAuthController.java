package com.resqhub.spring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.StatsController;
import com.resqhub.dao.DisasterDAO;
import com.resqhub.dao.ShelterDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.dao.VolunteerDAO;
import com.resqhub.dao.DonationDAO;
import com.resqhub.dao.DonorDAO;
import com.resqhub.dao.VolunteerSkillDAO;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.Donation;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;
import com.resqhub.model.Donor;
import com.resqhub.model.DonorType;
import com.resqhub.model.EmergencyRole;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.User;
import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerAvailability;
import com.resqhub.model.VolunteerSkill;
import com.resqhub.util.PasswordUtil;
import com.resqhub.util.ValidationUtil;

@Controller
public class SpringAuthController extends BaseWebController {

    private final AuthController authController = new AuthController();
    private final StatsController statsController = new StatsController();
    private final DisasterDAO disasterDao = new DisasterDAO();
    private final ShelterDAO shelterDao = new ShelterDAO();
    private final UserDAO userDao = new UserDAO();
    private final VolunteerDAO volunteerDao = new VolunteerDAO();
    private final VolunteerSkillDAO volunteerSkillDao = new VolunteerSkillDAO();
    private final DonorDAO donorDao = new DonorDAO();
    private final DonationDAO donationDao = new DonationDAO();

    @GetMapping("/")
    public String index(Model model,
                        @RequestParam(value = "volunteerSubmitted", required = false) Boolean volunteerSubmitted,
                        @RequestParam(value = "registeredEmail", required = false) String registeredEmail,
                        @RequestParam(value = "donationSubmitted", required = false) Boolean donationSubmitted) {
        try {
            ActionResult r = statsController.getSnapshot();
            if (r.isSuccess() && r.getData() != null) {
                model.addAttribute("stats", r.getData());
            }
        } catch (Exception ignored) {
        }

        try {
            List<Disaster> disasters = disasterDao.findAll();
            model.addAttribute("disasters", disasters);
        } catch (Exception e) {
            model.addAttribute("disasters", Collections.emptyList());
        }

        try {
            List<Shelter> shelters = shelterDao.findAll();
            model.addAttribute("shelters", shelters);
        } catch (Exception e) {
            model.addAttribute("shelters", Collections.emptyList());
        }

        try {
            List<User> admins = userDao.findByRole(RoleType.ADMIN);
            if (!admins.isEmpty()) {
                model.addAttribute("adminContact", admins.get(0));
            }
            List<User> officers = userDao.findByRole(RoleType.RESCUE_OFFICER);
            if (!officers.isEmpty()) {
                model.addAttribute("rescueContact", officers.get(0));
            }
            List<User> campManagers = userDao.findByRole(RoleType.CAMP_MANAGER);
            if (!campManagers.isEmpty()) {
                model.addAttribute("campContact", campManagers.get(0));
            }
        } catch (Exception ignored) {
        }

        if (!model.containsAttribute("volunteerPopupMessage") && Boolean.TRUE.equals(volunteerSubmitted)) {
            String targetEmail = (registeredEmail != null && !registeredEmail.isBlank()) ? registeredEmail.trim() : "your registered email";
            model.addAttribute("volunteerPopupMessage",
                    "Volunteer application submitted successfully! Your profile has been sent for admin verification. Once verified, your login credentials will be dispatched to " + targetEmail + ".");
        }

        if (!model.containsAttribute("donationSuccessMessage") && Boolean.TRUE.equals(donationSubmitted)) {
            model.addAttribute("donationSuccessMessage",
                    "Thank you for your generous contribution to ResQHub Relief Operations! Your support directly empowers frontline rescue operations and relief shelters.");
        }

        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String handleSignup(@RequestParam("username") String username,
                               @RequestParam("password") String password,
                               @RequestParam("confirmPassword") String confirmPassword,
                               @RequestParam("fullName") String fullName,
                               @RequestParam("email") String email,
                               @RequestParam(value = "phone", required = false) String phone,
                               @RequestParam(value = "mobile_no", required = false) String mobileNo,
                               Model model,
                               RedirectAttributes ra) {
        String effectivePhone = (phone != null && !phone.isBlank()) ? phone.trim() : (mobileNo != null ? mobileNo.trim() : "");

        if (effectivePhone.isEmpty()) {
            model.addAttribute("errorMessage", "Phone number is required");
            model.addAttribute("username", username);
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", effectivePhone);
            model.addAttribute("mobile_no", effectivePhone);
            return "auth/signup";
        }

        if (!ValidationUtil.isValidPhone(effectivePhone)) {
            model.addAttribute("errorMessage", "phone must be 10 digits");
            model.addAttribute("username", username);
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", effectivePhone);
            model.addAttribute("mobile_no", effectivePhone);
            return "auth/signup";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match");
            model.addAttribute("username", username);
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", effectivePhone);
            model.addAttribute("mobile_no", effectivePhone);
            return "auth/signup";
        }

        ActionResult r = authController.registerCitizen(username.trim(), password, fullName.trim(), email.trim(), effectivePhone);
        if (r.isSuccess()) {
            ra.addFlashAttribute("flashType", "success");
            ra.addFlashAttribute("flashMessage", r.getMessage());
            return "redirect:/login";
        } else {
            model.addAttribute("errorMessage", r.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", effectivePhone);
            model.addAttribute("mobile_no", effectivePhone);
            return "auth/signup";
        }
    }

    @GetMapping("/volunteer-registration")
    public String volunteerRegistrationPage(Model model) {
        model.addAttribute("emergencyRoles", EmergencyRole.values());
        model.addAttribute("availabilities", VolunteerAvailability.values());
        return "auth/volunteer-signup";
    }

    @PostMapping("/volunteer-registration")
    public String handleVolunteerRegistration(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "contactNumber", required = false) String contactNumber,
            @RequestParam(value = "mobile_no", required = false) String mobileNo,
            @RequestParam("location") String location,
            @RequestParam(value = "emergencyRole", required = false) String emergencyRoleStr,
            @RequestParam(value = "skills", required = false) String skills,
            @RequestParam(value = "availability", required = false) String availabilityStr,
            Model model,
            RedirectAttributes ra) {

        String effectivePhone = (contactNumber != null && !contactNumber.isBlank())
                ? contactNumber.trim()
                : (mobileNo != null ? mobileNo.trim() : "");

        model.addAttribute("emergencyRoles", EmergencyRole.values());
        model.addAttribute("availabilities", VolunteerAvailability.values());
        model.addAttribute("fullName", fullName);
        model.addAttribute("email", email);
        model.addAttribute("contactNumber", effectivePhone);
        model.addAttribute("mobile_no", effectivePhone);
        model.addAttribute("location", location);
        model.addAttribute("skills", skills);
        model.addAttribute("selectedRole", emergencyRoleStr);
        model.addAttribute("selectedAvailability", availabilityStr);

        if (fullName == null || fullName.trim().isEmpty() || !ValidationUtil.isValidName(fullName)) {
            model.addAttribute("errorMessage", "Please provide a valid full name (letters only).");
            return "auth/volunteer-signup";
        }

        if (effectivePhone.isEmpty() || !ValidationUtil.isValidPhone(effectivePhone)) {
            model.addAttribute("errorMessage", "Contact number must be exactly 10 digits.");
            return "auth/volunteer-signup";
        }

        if (email == null || !ValidationUtil.isValidEmail(email)) {
            model.addAttribute("errorMessage", "Please provide a valid email address.");
            return "auth/volunteer-signup";
        }

        if (location == null || location.trim().isEmpty()) {
            model.addAttribute("errorMessage", "Location/District cannot be empty.");
            return "auth/volunteer-signup";
        }

        try {
            for (Volunteer existing : volunteerDao.findAll()) {
                if (existing.getContactNumber().equals(effectivePhone)) {
                    model.addAttribute("errorMessage", "A volunteer is already registered with this contact number (" + effectivePhone + ").");
                    return "auth/volunteer-signup";
                }
            }

            // 1. Resolve EmergencyRole and VolunteerAvailability
            EmergencyRole emRole = null;
            if (emergencyRoleStr != null && !emergencyRoleStr.isBlank()) {
                try {
                    emRole = EmergencyRole.valueOf(emergencyRoleStr.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            if (emRole == null) {
                emRole = EmergencyRole.GENERAL;
            }

            VolunteerAvailability volAvail = VolunteerAvailability.AVAILABLE;
            if (availabilityStr != null && !availabilityStr.isBlank()) {
                try {
                    volAvail = VolunteerAvailability.valueOf(availabilityStr.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            // 2. Create Volunteer record with userId = null (awaiting admin verification & credential assignment)
            Volunteer v = new Volunteer(ValidationUtil.clean(fullName), effectivePhone, ValidationUtil.clean(location));
            v.setEmail(ValidationUtil.clean(email));
            v.setUserId(null);
            v.setSkills(ValidationUtil.clean(skills));
            v.setAvailability(volAvail);
            v.setEmergencyRole(emRole);
            v.setMaxWorkload(3);

            Volunteer savedVolunteer = volunteerDao.save(v);

            // 3. Save individual skills to volunteer_skills if provided
            if (skills != null && !skills.isBlank() && savedVolunteer != null && savedVolunteer.getId() != null) {
                String[] skillParts = skills.split("[,;]+");
                for (String sp : skillParts) {
                    String cleanSkill = sp.trim();
                    if (!cleanSkill.isEmpty() && cleanSkill.length() <= 100) {
                        try {
                            volunteerSkillDao.save(new VolunteerSkill(savedVolunteer.getId(), cleanSkill));
                        } catch (Exception ignored) {}
                    }
                }
            }

            String popupMsg = "Volunteer application submitted successfully! Your profile has been sent for admin verification. Once verified, your login credentials will be dispatched to " + email.trim() + ".";
            ra.addFlashAttribute("volunteerPopupMessage", popupMsg);
            ra.addAttribute("volunteerSubmitted", "true");
            ra.addAttribute("registeredEmail", email.trim());
            return "redirect:/";

        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Error during volunteer registration: " + ex.getMessage());
            return "auth/volunteer-signup";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPage() {
        return "auth/forgot";
    }

    @PostMapping("/forgot-password")
    public String handleForgot(@RequestParam("username") String username,
                               @RequestParam("email") String email,
                               @RequestParam("newPassword") String newPassword,
                               @RequestParam("confirmPassword") String confirmPassword,
                               Model model,
                               RedirectAttributes ra) {
        ActionResult r = authController.resetForgottenPassword(username.trim(), email.trim(), newPassword, confirmPassword);
        if (r.isSuccess()) {
            ra.addFlashAttribute("flashType", "success");
            ra.addFlashAttribute("flashMessage", r.getMessage());
            return "redirect:/login";
        } else {
            model.addAttribute("errorMessage", r.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "auth/forgot";
        }
    }

    @GetMapping("/setup")
    public String setupPage(Model model) {
        try {
            if (!userDao.findAll().isEmpty()) {
                return "redirect:/login";
            }
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Database check failed");
        }
        return "auth/setup";
    }

    @PostMapping("/setup")
    public String handleSetup(@RequestParam("username") String username,
                              @RequestParam("password") String password,
                              @RequestParam("confirmPassword") String confirmPassword,
                              @RequestParam("fullName") String fullName,
                              @RequestParam("email") String email,
                              @RequestParam(value = "phone", required = false) String phone,
                              Model model,
                              RedirectAttributes ra) {
        try {
            if (!userDao.findAll().isEmpty()) {
                return "redirect:/login";
            }

            String problem = null;
            if (!ValidationUtil.isValidUsername(username)) {
                problem = "Username must be 4-50 letters, digits or underscores";
            } else if (!ValidationUtil.isValidName(fullName)) {
                problem = "Full name is invalid";
            } else if (!ValidationUtil.isValidEmail(email)) {
                problem = "Email is invalid";
            } else if (phone != null && !phone.isBlank() && !ValidationUtil.isValidPhone(phone)) {
                problem = "Phone must be 10 digits";
            } else if (!ValidationUtil.isValidPassword(password)) {
                problem = "Password must be at least 8 characters and contain letters and digits";
            } else if (!password.equals(confirmPassword)) {
                problem = "Passwords do not match";
            }

            if (problem != null) {
                model.addAttribute("errorMessage", problem);
                model.addAttribute("username", username);
                model.addAttribute("fullName", fullName);
                model.addAttribute("email", email);
                model.addAttribute("phone", phone);
                return "auth/setup";
            }

            User admin = new User(fullName, phone, email);
            admin.setUsername(username);
            admin.setPasswordHash(PasswordUtil.hash(password));
            admin.setRole(RoleType.ADMIN);
            admin.setAccountStatus(AccountStatus.ACTIVE);
            userDao.save(admin);

            ra.addFlashAttribute("flashType", "success");
            ra.addFlashAttribute("flashMessage", "Administrator account created successfully. Please log in.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Could not create administrator: " + e.getMessage());
            return "auth/setup";
        }
    }

    @GetMapping("/donate")
    public String donatePage() {
        return "redirect:/#donateModal";
    }

    @PostMapping("/donate")
    public String handleDonate(@RequestParam("fullName") String fullName,
                               @RequestParam(value = "contactNumber", required = false) String contactNumber,
                               @RequestParam(value = "email", required = false) String email,
                               @RequestParam(value = "location", required = false) String location,
                               @RequestParam(value = "donorType", defaultValue = "INDIVIDUAL") String donorTypeStr,
                               @RequestParam(value = "donationType", defaultValue = "CASH") String donationTypeStr,
                               @RequestParam(value = "amount", required = false) BigDecimal amount,
                               @RequestParam(value = "materialName", required = false) String materialName,
                               @RequestParam(value = "quantity", required = false) Integer quantity,
                               @RequestParam(value = "description", required = false) String description,
                               RedirectAttributes ra) {
        try {
            if (fullName == null || fullName.trim().isEmpty()) {
                ra.addFlashAttribute("donationErrorMessage", "Please enter your name or organization name.");
                return "redirect:/#donateModal";
            }

            DonorType donorType = DonorType.INDIVIDUAL;
            try {
                if (donorTypeStr != null && !donorTypeStr.isBlank()) {
                    donorType = DonorType.valueOf(donorTypeStr.trim().toUpperCase());
                }
            } catch (Exception ignored) {}

            DonationType donationType = DonationType.CASH;
            try {
                if (donationTypeStr != null && !donationTypeStr.isBlank()) {
                    donationType = DonationType.valueOf(donationTypeStr.trim().toUpperCase());
                }
            } catch (Exception ignored) {}

            Donor donor = new Donor();
            donor.setFullName(fullName.trim());
            donor.setContactNumber(contactNumber != null ? contactNumber.trim() : "");
            donor.setEmail(email != null ? email.trim() : "");
            donor.setLocation(location != null && !location.isBlank() ? location.trim() : "Kerala, India");
            donor.setDonorType(donorType);
            donor = donorDao.save(donor);

            Donation donation = new Donation();
            donation.setDonorId(donor.getId());
            donation.setDonationType(donationType);
            donation.setStatus(DonationStatus.RECEIVED);
            donation.setDonatedAt(LocalDateTime.now());
            donation.setDescription(description != null ? description.trim() : "");

            if (donationType == DonationType.CASH) {
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    amount = new BigDecimal("500.00");
                }
                donation.setAmount(amount);
            } else {
                donation.setMaterialName(materialName != null && !materialName.isBlank() ? materialName.trim() : "Emergency Relief Supplies");
                donation.setQuantity(quantity != null && quantity > 0 ? quantity : 1);
            }
            donationDao.save(donation);

            String msg = donationType == DonationType.CASH
                    ? "Thank you, " + donor.getFullName() + "! Your donation of \u20B9" + donation.getAmount() + " has been successfully recorded to support emergency relief operations."
                    : "Thank you, " + donor.getFullName() + "! Your relief aid pledge (" + donation.getQuantity() + "x " + donation.getMaterialName() + ") has been recorded. Our logistics team will connect with you.";

            ra.addFlashAttribute("donationSuccessMessage", msg);
            return "redirect:/?donationSubmitted=true#overview";
        } catch (Exception e) {
            ra.addFlashAttribute("donationErrorMessage", "Could not process donation: " + e.getMessage());
            return "redirect:/#donateModal";
        }
    }
}
