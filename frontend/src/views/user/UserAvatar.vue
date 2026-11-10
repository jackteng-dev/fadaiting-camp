<script setup lang="ts">
import { ref} from 'vue'
import default_avatar from '@/assets/images/user/avatar-1.jpg'
import { useUserInfoStore } from '@/stores/userInfo'
import { ElMessage } from 'element-plus'
import { userAvatarUpdateService } from '@/api/user' // 你自己的上傳 API

const userInfo = useUserInfoStore()

const previewUrl = ref(userInfo.info?.userPic || '')
const selectedFile = ref<File | null>(null)

// 處理選圖
const handleUpload = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.error('請選擇圖片檔案')
    return
  }

  selectedFile.value = file

  // 預覽
  const reader = new FileReader()
  reader.onload = () => {
    previewUrl.value = reader.result as string
  }
  reader.readAsDataURL(file)
}

// 點擊上傳按鈕，送到後端
const uploadToServer = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('請先選擇圖片')
    return
  }
  const formData = new FormData()
  formData.append('file', selectedFile.value)

  try {
    const res = await userAvatarUpdateService(formData)
    const newUrl = res.data
    previewUrl.value = newUrl
    userInfo.info!.userPic = newUrl    // 更新 pinia
    ElMessage.success('上傳成功')
  } catch (e) {
    ElMessage.error('上傳失敗')
  }
}
</script>

<template>
  <div class="row g-2 g-md-3 bg-white border rounded mt-3 mx-sm-1 px-3 py-3">
    <div class="col-12 text-center">
      <img
        :src="previewUrl || default_avatar"
        class="rounded-circle border"
        style="width: 120px; height: 120px; object-fit: cover;"
        alt="頭像"
      />
    </div>
    <div class="col-12 col-md-6 d-grid">
      <input type="file" class="form-control" accept="image/*" @change="handleUpload" />
    </div>
    <div class="col-12 col-md-6 d-grid">
      <button class="btn btn-dark" @click="uploadToServer">上傳頭像</button>
    </div>
  </div>
</template>
