<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { UserInfo } from '@/types/user'
import { useUserInfoStore } from '@/stores/userInfo'
import { getBackendUser } from '@/api/user.ts'
import { ElMessageBox, ElMessage } from "element-plus";
import { userInfoUpdateService } from '@/api/user.ts'
import cloneDeep from 'lodash/cloneDeep'
import UserAvatar from '@/views/user/UserAvatar.vue'

const userInfo = useUserInfoStore()
const userInfoData = ref<UserInfo>({} as UserInfo)
const originalData = ref<UserInfo>({} as UserInfo)

const loadUser = async () => {
    const response = await getBackendUser(userInfo.info!.username)
    console.log(response.data)
    userInfoData.value = response.data
    originalData.value = cloneDeep(response.data)
}

onMounted(loadUser)

const update = async () => {
    //提示用戶 確認框
    ElMessageBox.confirm("你確認要修改嗎?", "溫馨提示", {
        confirmButtonText: "確定",
        cancelButtonText: "取消",
        type: "warning",
    }).then(async () => {
        await userInfoUpdateService(userInfoData.value);
        userInfo.setInfo(cloneDeep(userInfoData.value));
        loadUser()
        ElMessage({
            type: "success",
            message: "修改成功",
        });
    });
}

const resetForm = () => {
    userInfoData.value = cloneDeep(originalData.value)
}
</script>

<template>
    <!-- page-header -->
    <div class="page-header">
        <div class="page-block">
            <ul class="breadcrumb">
                <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
                <li class="breadcrumb-item" aria-current="page">個人中心</li>
                <li class="breadcrumb-item"><router-link to="/admin/user/info">基本資料</router-link></li>
            </ul>
            <h4 class="pb-2 border-bottom mb-3"><i class="bi bi-feather me-2"></i>基本資料</h4>
        </div>
    </div>
    <!-- page-header-end -->
     <UserAvatar />
    <!-- main -->
    <form @submit.prevent="update">
        <div class="row g-2 g-md-3 bg-white border rounded mt-3 mx-sm-1 px-3">
            <div class="col-md-6">
                <label for="username" class="form-label text-muted">帳號</label>
                <p>{{ userInfoData.username }}</p>
                <!-- <input id="username" type='text' class="form-control"  v-model="userInfoData.username"> -->
            </div>
            <div class="col-md-6">
                <label for="username" class="form-label text-muted">角色</label>
                <p v-for="list1 in userInfoData.role" :key="list1.id">{{ list1.name }}</p>
                <!-- <input id="username" type='text' class="form-control"  v-model="userInfoData.username"> -->
            </div>
            <div class="col-md-6">
                <label for="firstName" class="form-label text-muted">姓氏</label>
                <input id="firstName" type='text' class="form-control" v-model="userInfoData.firstName">
            </div>
            <div class="col-md-6">
                <label for="lastName" class="form-label text-muted">名字</label>
                <input id="lastName" type='text' class="form-control" v-model="userInfoData.lastName">
            </div>
            <div class="col-md-6">
                <label for="email" class="form-label text-muted">電子信箱</label>
                <input id="email" type='text' class="form-control" v-model="userInfoData.email">
            </div>
            <div class="col-md-6">
                <label for="telephone" class="form-label text-muted">電話號碼</label>
                <input id="telephone" type='text' class="form-control" v-model="userInfoData.telephone">
            </div>
            <div class="col-md-12">
                <label for="username" class="form-label text-muted">使用狀態</label>
                <p>{{ userInfoData.isStop == 'F' ? '啟用' : '停用' }}</p>
                <!-- <input id="username" type='text' class="form-control"  v-model="userInfoData.username"> -->
            </div>
            <!--button-->
            <div class="col-12 my-3">
                <div class="row">
                    <div class="col-md-6 d-grid mb-2"><button class="btn btn-secondary" type="button"
                            @click="resetForm">清除重填</button></div>
                    <div class="col-md-6 d-grid mb-2"><button class="btn btn-dark" type="submit">修改</button></div>
                </div>
            </div>
        </div>
    </form>
    <!-- main -->
</template>
