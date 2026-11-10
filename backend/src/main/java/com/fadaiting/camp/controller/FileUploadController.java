package com.fadaiting.camp.controller;

import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.Result;
import com.fadaiting.camp.pojo.User;
import com.fadaiting.camp.service.UserService;
import com.fadaiting.camp.service.ObjectStorageService;
import com.fadaiting.camp.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
public class FileUploadController {
    @Autowired
    private ObjectStorageService objectStorageService;

    @Autowired
    private UserService userService;

    //上傳用戶頭像
    @PatchMapping("/upload/userAvatar")
    public Result<String> createAvatar(MultipartFile file, @AuthenticationPrincipal CustomUserDetails currentUser) throws Exception {
        String username = currentUser.getUsername();
        User user = userService.findByUserName(username);
        //取得原有圖片url
        String filename = null;
        String url = user.getUserPic();
        if (url != null && !url.isBlank()) {
            String path = java.net.URI.create(url).getPath();
            String candidate = path.substring(path.lastIndexOf('/') + 1);
            if (candidate.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,199}") && !candidate.contains("..")) filename = candidate;
        }
        // 上傳到 Camp 專用 SeaweedFS S3。
        String avatarUrl = objectStorageService.fileupload(file, "user-avatar", filename);
        //更新資料庫
        userService.updateAvatar(avatarUrl,currentUser.getId());
        return Result.success(avatarUrl);
    }

    @PostMapping("/upload/updateAvatar")
    public Result<String> updateAvatar(MultipartFile file,@AuthenticationPrincipal CustomUserDetails currentUser) throws Exception {
        //        System.out.println(file);
        String avatarUrl = objectStorageService.fileupload(file, "user-avatar", null);
        userService.updateAvatar(avatarUrl,currentUser.getId());
        return Result.success(avatarUrl);
    }

    @PostMapping("/upload/admin-image")
    public Result<String> uploadAdminImage(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "camp-images") String bucket
    ) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("請選擇圖片檔案");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("只能上傳圖片檔案");
        }
        String imageUrl = objectStorageService.fileupload(file, bucket, null);
        return Result.success(imageUrl);
    }

    @GetMapping("/upload")
    public Result<String> uploadImageView(String path) throws IOException {
        File imageFile = new File(path);
        byte[] imagesBytes = Files.readAllBytes(imageFile.toPath());
        String base64Image = Base64.getEncoder().encodeToString(imagesBytes);
        return Result.success("data:image/" + path.substring(path.lastIndexOf(".") + 1) + ";base64," + base64Image);
    }
}
