package org.testing.pt.gatling.controller.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testing.pt.gatling.model.FileInfo;
import org.testing.pt.gatling.service.FileScanner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for the FileController API class.
 */
@ExtendWith(MockitoExtension.class)
public class FileControllerTest {
    
    @Mock
    private FileScanner fileScanner;
    
    private FileController fileController;
    private MockMvc mockMvc;
    
    @BeforeEach
    public void setUp() {
        fileController = new FileController();
        ReflectionTestUtils.setField(fileController, "fileScanner", fileScanner);
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    }
    
    @Test
    public void testGetAllFiles() throws Exception {
        List<FileInfo> files = Arrays.asList(
            createFileInfo("test1.jar"),
            createFileInfo("test2.jar")
        );
        
        when(fileScanner.getAllFiles()).thenReturn(files);
        
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("test1.jar"))
                .andExpect(jsonPath("$[1].name").value("test2.jar"));
        
        verify(fileScanner).getAllFiles();
    }
    
    // Note: The actual FileController only has getAllFiles() method
    // File upload/delete functionality is handled elsewhere
    
    private FileInfo createFileInfo(String name) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(name);
        return fileInfo;
    }
}