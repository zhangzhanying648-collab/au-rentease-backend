package com.zzy.aurenteasebackend.controller;

import com.zzy.aurenteasebackend.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    //大厂标准：前端传一个文件名，后端光速返回签名令牌，不吃任何文件流量

    @RequestMapping(value = "/presigned-upload", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<FileStorageService.PresignedUrlResponse> getUploadPresignedUrl(
            @RequestParam("filename") String filename){
        FileStorageService.PresignedUrlResponse response=fileStorageService.generateUploadUrl(filename);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/view-url")
    public ResponseEntity<String> getViewUrl(@RequestParam("objectKey") String objectKey) {
        String url = fileStorageService.generateViewUrl(objectKey); //[cite: 2]
        return ResponseEntity.ok(url);
    }
}
