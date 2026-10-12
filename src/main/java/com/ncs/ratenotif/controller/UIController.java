package com.ncs.ratenotif.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.ncs.ratenotif.response.ClientResponse;
import com.ncs.ratenotif.response.dto.ResponseDTO;
import com.ncs.ratenotif.user.request.LoginRequest;
import com.ncs.ratenotif.user.request.SignupRequest;

@Controller
public class UIController {

    @Autowired
    private RestTemplate restTemplate;

    private final String BASE_URL = "http://localhost:8089";

    
    @GetMapping("/ui-login")
    public String uiLogin(
            @RequestParam String username,
            @RequestParam String role,
            HttpSession session) {

        session.setAttribute("LOGGED_IN_USER", username);
        session.setAttribute("ROLE", role);

        return "redirect:/";
    }
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signup(HttpSession session) {
        // only ADMIN can create users
        if (!"SUPER_ADMIN".equals(session.getAttribute("ROLE"))) {
            return "redirect:/";
        }
        return "signup";
    }

    /* =========================
       LOGIN ACTION
       ========================= */

    @PostMapping("/do-login")
    public String doLogin(
            @RequestParam String userName,
            @RequestParam String password,
            HttpSession session
    ) {

        LoginRequest req = new LoginRequest();
        req.setUserName(userName);
        req.setPassword(password);

        HttpEntity<LoginRequest> entity = new HttpEntity<>(req);

        try {
            ResponseDTO<Map<String, Object>> response =
                    restTemplate.exchange(
                            BASE_URL + "/user/login",
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<ResponseDTO<Map<String, Object>>>() {}
                    ).getBody();

            if (response == null || response.getData() == null) {
                return "redirect:/login?error";
            }

            Map<String, Object> user = response.getData();

            // 🔥 SESSION SET FROM BACKEND DATA
            session.setAttribute("LOGGED_IN_USER", user.get("username"));
            session.setAttribute("ROLE", user.get("role"));

            return "redirect:/";

        } catch (Exception e) {
            return "redirect:/login?error";
        }
    }

    /* =========================
       SIGNUP ACTION (ADMIN ONLY)
       ========================= */

    @PostMapping("/do-signup")
    public String doSignup(
            @RequestParam String userName,
            @RequestParam String fullName,
            @RequestParam String password,
            @RequestParam String role,
            HttpSession session
    ) {

        if (!"SUPER_ADMIN".equals(session.getAttribute("ROLE"))) {
            return "redirect:/";
        }

        SignupRequest req = new SignupRequest();
        req.setUserName(userName);
        req.setFullName(fullName);
        req.setPassword(password);
        req.setRole(Enum.valueOf(
                com.ncs.ratenotif.user.entity.Roles.class, role));

        HttpEntity<SignupRequest> entity = new HttpEntity<>(req);

        restTemplate.exchange(
                BASE_URL + "/user/signup",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<ResponseDTO<Object>>() {}
        );

        return "redirect:/";
    }

    /* =========================
       LOGOUT
       ========================= */

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /* =========================
       DASHBOARD (HOME)
       ========================= */

    @GetMapping("/")
    public String home(
            @RequestParam(defaultValue = "email") String page,
            @RequestParam(required = false) Long id,
            Model model,
            HttpSession session
    ) {

        if (session.getAttribute("LOGGED_IN_USER") == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", session.getAttribute("LOGGED_IN_USER"));
        model.addAttribute("role", session.getAttribute("ROLE"));
        model.addAttribute("page", page);

        if ("clients".equals(page)) {

            String username = (String) session.getAttribute("LOGGED_IN_USER");
            String role  = (String) session.getAttribute("ROLE");

            ResponseDTO<List<ClientResponse>> response =
                    restTemplate.exchange(
                            BASE_URL + "/client/getAllClient/" + username + "/" + role,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ResponseDTO<List<ClientResponse>>>() {}
                    ).getBody();

            if (response != null) {
                model.addAttribute("clients", response.getData());
            }
        }


        if ("client-details".equals(page) && id != null) {

            String username = (String) session.getAttribute("LOGGED_IN_USER");
            String role     = (String) session.getAttribute("ROLE");

            ResponseDTO<ClientResponse> response =
                    restTemplate.exchange(
                            BASE_URL + "/client/getClientById/"
                                    + id + "/" + username + "/" + role,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ResponseDTO<ClientResponse>>() {}
                    ).getBody();

            if (response != null) {
                model.addAttribute("client", response.getData());
            }
        }


        return "index";
    }
}