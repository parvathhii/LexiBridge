package com.lexibridge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

@Controller
public class HomeController {

    private final DataSource dataSource;

    public HomeController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "LexiBridge");
        model.addAttribute("phase", "Phase 1: Foundation & MySQL Database Connection");
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            model.addAttribute("dbStatus", "CONNECTED");
            model.addAttribute("dbName", metaData.getDatabaseProductName());
            model.addAttribute("dbVersion", metaData.getDatabaseProductVersion());
            model.addAttribute("dbCatalog", connection.getCatalog());
        } catch (Exception e) {
            model.addAttribute("dbStatus", "DISCONNECTED: " + e.getMessage());
            model.addAttribute("dbName", "Unknown");
            model.addAttribute("dbVersion", "N/A");
            model.addAttribute("dbCatalog", "None");
        }
        
        return "index";
    }
}
