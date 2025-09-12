package org.testing.pt.gatling.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for handling login page requests.
 */
@Controller
public class LoginController {
    
    /**
     * Displays the login page.
     * 
     * @param error optional error parameter
     * @param logout optional logout parameter
     * @param model the model for the view
     * @return the login template name
     */
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        
        return "login";
    }
}