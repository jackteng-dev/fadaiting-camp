// //定義store
// import { defineStore } from 'pinia'
// import { ref } from 'vue'
// import axios from 'axios'; // 确保导入axios
// //第一個參數:名字，唯一性
// //第二個參數:函數，函數內部可以定義狀態的所有內容
// //返回值:函數
// export const useTokenStore = defineStore("token", () => {
//     //1.響應式變量
//     const accessToken = ref<string>('');
//     //2.定義一個函數，修改token的值
//     const setToken = (newToken: string,rememberMe: boolean) => {
//         accessToken.value = newToken;
//         if(rememberMe){
//             localStorage.setItem('accessToken', newToken);
//         }else{
//             sessionStorage.setItem('accessToken', newToken);
//         }
//     }
//     //3.移除token的值
//     const removeToken = () => {
//         accessToken.value = '';
//         localStorage.removeItem('accessToken');
//         sessionStorage.removeItem('accessToken');
//     }
//     const refreshAccessToken = async () => {
//         console.log('kkk');
//         try {
//             const result = await axios.post('/api/token/refresh', { withCredentials: true });
//             const newAccessToken = result.data.data;
//             console.log('newAccessToken:::' + newAccessToken);
//             const rememberMe = !!localStorage.getItem('accessToken');
//             setToken(newAccessToken,rememberMe);
//         } catch (error) {
//             console.error('訪問令牌錯誤', error);
//             removeToken();
//             throw error;
//         }
//     }
//     const initToken = () => {
//         const savedToken = localStorage.getItem("accessToken") || sessionStorage.getItem("accessToken");
//         console.log('lltoken:::'+localStorage.getItem("accessToken"));
//         console.log('sstoken:::'+sessionStorage.getItem("accessToken"));
        
//         // if (savedToken) {
//             // accessToken.value = savedToken;
//             accessToken.value = savedToken ?? '';
//             // console.log('accessToken.value:::'+accessToken.value);

//         // }
//     };
//     return {
//         accessToken, setToken, removeToken, refreshAccessToken,initToken
//     }
// }
// );