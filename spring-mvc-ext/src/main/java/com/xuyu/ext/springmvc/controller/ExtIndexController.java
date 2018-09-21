package com.xuyu.ext.springmvc.controller;

import com.xuyu.ext.springmvc.exannotation.ExtController;
import com.xuyu.ext.springmvc.exannotation.ExtRequestMapping;

@ExtController
@ExtRequestMapping("/ext")
public class ExtIndexController {

	@ExtRequestMapping("/text")
	public String test() {
		System.out.println(" ÷–¥springmvc");
		return "index";
	}
}
