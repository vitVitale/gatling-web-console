package org.testing.pt.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testing.pt.server.model.FileInfo;
import org.testing.pt.server.service.FileScanner;

import java.util.List;

/**
 * REST controller for accessing file information.
 */
@RestController
@RequestMapping("/api")
public class FileController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    @Autowired
    private FileScanner fileScanner;
    
    /**
     * Gets all files in the simulations folder.
     * 
     * @return a list of FileInfo objects
     */
    @GetMapping("/files")
    public ResponseEntity<List<FileInfo>> getAllFiles() {
        logger.debug("Getting all files");
        List<FileInfo> files = fileScanner.getAllFiles();
        return ResponseEntity.ok(files);
    }
}