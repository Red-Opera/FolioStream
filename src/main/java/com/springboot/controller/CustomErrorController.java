package com.springboot.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;

@Controller
public class CustomErrorController implements ErrorController 
{
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) 
    {
        Object status = request.getAttribute("javax.servlet.error.status_code");
        Object message = request.getAttribute("javax.servlet.error.message");
        Object exception = request.getAttribute("javax.servlet.error.exception");
        Object uri = request.getAttribute("javax.servlet.error.request_uri");

        model.addAttribute("status", status);
        model.addAttribute("message", message);
        model.addAttribute("path", uri);

        if (exception != null && exception instanceof Throwable) 
        {
            String stackTrace = Arrays.toString(((Throwable) exception).getStackTrace());
            
            model.addAttribute("exception", exception.toString());
            model.addAttribute("stackTrace", stackTrace);
        }

        return "error";
    }
} 