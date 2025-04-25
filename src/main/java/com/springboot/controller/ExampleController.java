package com.springboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ExampleController 
{
	@GetMapping("/test")
	public String requestMethod()
	{
		return "test";
	}
}
