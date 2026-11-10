package com.fadaiting.camp;

import com.fadaiting.camp.controller.FileUploadController;
import com.fadaiting.camp.pojo.CustomUserDetails;
import com.fadaiting.camp.pojo.User;
import com.fadaiting.camp.service.ObjectStorageService;
import com.fadaiting.camp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileUploadControllerTest {
    private final ObjectStorageService storage = mock(ObjectStorageService.class);
    private final UserService users = mock(UserService.class);
    private final FileUploadController controller = new FileUploadController();
    private final MockMultipartFile image = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
    private final CustomUserDetails actor = new CustomUserDetails("synthetic", 123, List.of());

    @BeforeEach void setup() {
        ReflectionTestUtils.setField(controller, "objectStorageService", storage);
        ReflectionTestUtils.setField(controller, "userService", users);
    }
    @Test void firstAvatarWithNullUrlDoesNotDereferenceNull() throws Exception {
        when(users.findByUserName("synthetic")).thenReturn(new User());
        when(storage.fileupload(image, "user-avatar", null)).thenReturn("/objects/user-avatar/new?signature");
        assertEquals("/objects/user-avatar/new?signature", controller.createAvatar(image, actor).getData());
        verify(users).updateAvatar("/objects/user-avatar/new?signature", 123);
    }
    @Test void legacySignedAvatarRetainsObjectKey() throws Exception {
        User user = new User();user.setUserPic("/objects/user-avatar/original-key?old-signature");
        when(users.findByUserName("synthetic")).thenReturn(user);
        controller.createAvatar(image, actor);
        verify(storage).fileupload(image, "user-avatar", "original-key");
    }
    @Test void avatarWithoutSignedQueryDoesNotRequireQuestionMark() throws Exception {
        User user = new User();user.setUserPic("/objects/user-avatar/original-key");
        when(users.findByUserName("synthetic")).thenReturn(user);
        controller.createAvatar(image, actor);
        verify(storage).fileupload(image, "user-avatar", "original-key");
    }
    @Test void failedStorageDoesNotUpdateAvatarRecord() throws Exception {
        when(storage.fileupload(image, "user-avatar", null)).thenThrow(new IllegalStateException("合成儲存失敗"));
        assertThrows(IllegalStateException.class, () -> controller.updateAvatar(image, actor));
        verifyNoInteractions(users);
    }
}
