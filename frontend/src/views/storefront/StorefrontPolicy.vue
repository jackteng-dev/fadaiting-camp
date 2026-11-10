<script setup lang="ts">
import { computed } from "vue";
import { RouterLink, useRoute } from "vue-router";

type PolicyPage = {
  kicker: string;
  title: string;
  intro: string;
  sections: Array<{
    title: string;
    items: string[];
  }>;
};

const route = useRoute();

const pages: Record<string, PolicyPage> = {
  "/booking-guide": {
    kicker: "BOOKING GUIDE",
    title: "預約須知",
    intro: "送出預約後，營區會依日期、營位與現場安排進行人工確認；確認前請先保留交通與同行安排的彈性。",
    sections: [
      {
        title: "預約流程",
        items: [
          "線上送出預約單後，系統會保留訂單編號，營區端再進行人工確認。",
          "若日期、營位或租借用品需要調整，營區會以電話、Email 或官方帳號與你聯繫。",
          "付款完成不代表可跳過營區確認；最終入住安排仍以營區回覆為準。",
        ],
      },
      {
        title: "入住與離營",
        items: [
          "一般入住時間為 14:00 後，離營時間為 11:00 前；特殊需求請先於備註提出。",
          "車輛與人數請依預約內容入場，如需增加請提前聯繫營區。",
          "山區天候變化較快，建議出發前留意交通、雨具、保暖與照明用品。",
        ],
      },
      {
        title: "租借用品",
        items: [
          "帳棚、防水墊與其他用品可於預約時先加選，實際準備會由營區確認。",
          "租借用品數量以指定入住日期區間的可租庫存為準。",
          "若現場歸還有損壞或遺失，營區會依用品狀態與客人另行確認處理方式。",
        ],
      },
    ],
  },
  "/refund-policy": {
    kicker: "REFUND POLICY",
    title: "退款與取消政策",
    intro: "小型露營區排班、清潔與備品準備都需要提前安排；若行程有變，越早聯繫越能保留調整空間。",
    sections: [
      {
        title: "取消與改期",
        items: [
          "如需取消或改期，請提供訂單編號與聯絡電話，方便營區查詢紀錄。",
          "可改期名額會依實際空位、季節與用品庫存調整，並非所有日期都可直接保留原方案。",
          "若因天災、道路管制或營區安全考量無法入住，營區會主動聯繫調整或退款方式。",
        ],
      },
      {
        title: "退款處理",
        items: [
          "線上付款退款需依 LINE Pay、綠界或實際收款通道規則處理。",
          "人工轉帳或現金補款的退款，會由營區確認帳務後另行安排。",
          "退款紀錄與狀態會保留於後台，正式退款完成時間仍以金流或銀行作業為準。",
        ],
      },
      {
        title: "未付款訂單",
        items: [
          "付款請求逾時或取消後，仍可從查詢預約頁重新建立付款。",
          "長時間未付款且未與營區聯繫的訂單，營區可視情況釋出名額。",
          "若已付款但訂單仍顯示待確認，代表營區尚未完成人工確認，不代表付款失敗。",
        ],
      },
    ],
  },
  "/privacy-policy": {
    kicker: "PRIVACY",
    title: "隱私政策",
    intro: "我們只蒐集完成預約、聯繫、付款與入住安排所需的基本資料，並以營區營運需求為使用範圍。",
    sections: [
      {
        title: "資料蒐集",
        items: [
          "預約時會蒐集姓名、電話、Email、入住日期、人數、車數、備註與付款紀錄。",
          "如你使用 LINE 官方帳號或相關服務，可能會保存與訂單查詢或通知相關的識別資料。",
          "付款資料由金流服務商處理，本站僅保存交易狀態、金額與必要交易編號。",
        ],
      },
      {
        title: "資料使用",
        items: [
          "資料用於預約確認、付款通知、入住前提醒、現場安排與必要客服聯繫。",
          "營區不會將旅客資料販售給第三方。",
          "若依法令、金流查核或爭議處理需要，可能會保存必要紀錄。",
        ],
      },
      {
        title: "資料調整",
        items: [
          "若預約資料有誤，可透過查詢預約或直接聯繫營區修正。",
          "如需刪除或查詢個人資料，可提供訂單編號與聯絡電話供營區核對。",
          "正式上線前，隱私政策會再依實際通知通道與部署環境補齊。",
        ],
      },
    ],
  },
};

const page = computed(() => pages[route.path] || pages["/booking-guide"]);
</script>

<template>
  <main class="policy-page">
    <section class="policy-hero">
      <span>{{ page.kicker }}</span>
      <h1>{{ page.title }}</h1>
      <p>{{ page.intro }}</p>
    </section>

    <section class="policy-content" aria-label="政策內容">
      <article v-for="section in page.sections" :key="section.title" class="policy-section">
        <h2>{{ section.title }}</h2>
        <ul>
          <li v-for="item in section.items" :key="item">{{ item }}</li>
        </ul>
      </article>
    </section>

    <section class="policy-actions">
      <RouterLink to="/booking" class="primary-action">前往預約</RouterLink>
      <RouterLink to="/booking/lookup" class="secondary-action">查詢預約</RouterLink>
    </section>
  </main>
</template>

<style scoped>
.policy-page {
  width: min(1080px, calc(100% - 32px));
  margin: 36px auto 64px;
  color: #202821;
}

.policy-hero {
  padding: 42px 0 30px;
  border-bottom: 1px solid rgba(111, 117, 64, 0.16);
}

.policy-hero span {
  display: block;
  margin-bottom: 12px;
  color: #8b8f77;
  font-size: 0.78rem;
  letter-spacing: 0.16em;
}

.policy-hero h1 {
  margin: 0 0 14px;
  font-size: clamp(2.2rem, 4vw, 4.4rem);
  line-height: 1.05;
}

.policy-hero p {
  max-width: 760px;
  margin: 0;
  color: #66705e;
  line-height: 1.85;
}

.policy-content {
  display: grid;
  gap: 18px;
  margin-top: 28px;
}

.policy-section {
  padding: 26px 0;
  border-bottom: 1px solid rgba(111, 117, 64, 0.12);
}

.policy-section h2 {
  margin: 0 0 14px;
  font-size: 1.35rem;
}

.policy-section ul {
  display: grid;
  gap: 10px;
  margin: 0;
  padding-left: 1.25rem;
  color: #4f5c4b;
  line-height: 1.8;
}

.policy-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 30px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 0 22px;
  border-radius: 999px;
  font-weight: 700;
  text-decoration: none;
}

.primary-action {
  background: #6f7540;
  color: #fffdf7;
}

.secondary-action {
  border: 1px solid rgba(111, 117, 64, 0.24);
  color: #4f583f;
}

@media (max-width: 640px) {
  .policy-page {
    width: min(100%, calc(100% - 20px));
    margin-top: 22px;
  }

  .policy-hero {
    padding-top: 28px;
  }
}
</style>
