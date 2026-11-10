<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getCampSiteDetailService } from "@/api/camp";

type CampSite = {
  addressLine?: string;
  contactPhone?: string;
  contactEmail?: string;
  facebookUrl?: string;
};

const site = ref<CampSite | null>(null);

const loadFooterData = async () => {
  try {
    const siteRes = await getCampSiteDetailService();
    site.value = siteRes.data?.site ?? null;
  } catch {
    site.value = null;
  }
};

onMounted(loadFooterData);
</script>

<template>
  <footer class="storefront-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <div class="footer-brand-head">
          <img src="@/assets/images/brand/fadaiting-logo-mark.png" alt="發呆亭露營區">
          <strong>發呆亭露營區</strong>
        </div>
        <p>把山景、夜色、溪谷和剛剛好的慢節奏，留給真的想休息的人。</p>
      </div>

      <div class="footer-contact">
        <span class="footer-kicker">CONTACT</span>
        <div class="contact-list">
          <a class="contact-link" :href="`tel:${site?.contactPhone || '0900000000'}`">
            <i class="bi bi-telephone"></i>
            <span>{{ site?.contactPhone || "0900 000 000" }}</span>
          </a>
          <a class="contact-link" :href="`mailto:${site?.contactEmail || 'camp@example.com'}`">
            <i class="bi bi-envelope"></i>
            <span>{{ site?.contactEmail || "camp@example.com" }}</span>
          </a>
          <div class="contact-link static">
            <i class="bi bi-geo-alt"></i>
            <span>{{ site?.addressLine || "山嵐縣雲霧鄉清水村 8 號（虛構地址）" }}</span>
          </div>
          <a
            v-if="site?.facebookUrl"
            class="contact-link"
            :href="site.facebookUrl"
            target="_blank"
            rel="noreferrer"
          >
            <i class="bi bi-facebook"></i>
            <span>Facebook 粉專</span>
          </a>
        </div>
      </div>

      <nav class="footer-links" aria-label="頁尾導覽">
        <RouterLink to="/zones">營位介紹</RouterLink>
        <RouterLink to="/map">營區地圖</RouterLink>
        <RouterLink to="/booking">線上預約</RouterLink>
        <RouterLink to="/booking/lookup">查詢預約</RouterLink>
        <RouterLink to="/news">最新消息</RouterLink>
        <RouterLink to="/faq">常見問題</RouterLink>
        <RouterLink to="/booking-guide">預約須知</RouterLink>
        <RouterLink to="/refund-policy">退款政策</RouterLink>
        <RouterLink to="/privacy-policy">隱私政策</RouterLink>
      </nav>
    </div>
  </footer>
</template>

<style scoped>
.storefront-footer {
  padding: 0 0 28px;
}

.footer-inner {
  width: min(1520px, calc(100% - 32px));
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(300px, 0.95fr) auto;
  gap: 28px;
  align-items: start;
  padding: 28px;
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.92);
  border: 1px solid rgba(120, 111, 92, 0.1);
  color: #454d3f;
}

.footer-brand-head,
.footer-kicker {
  display: block;
}

.footer-brand-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.footer-brand-head img {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  object-fit: cover;
  background: #f4ecd6;
}

.footer-brand strong {
  font-size: 1rem;
}

.footer-brand p {
  margin: 0;
  max-width: 520px;
  line-height: 1.75;
  color: #666e61;
}

.footer-kicker {
  margin-bottom: 14px;
  color: #8e927f;
  font-size: 0.76rem;
  letter-spacing: 0.14em;
}

.contact-list {
  display: grid;
  gap: 10px;
}

.contact-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #4f583f;
  text-decoration: none;
  line-height: 1.7;
  word-break: break-word;
  transition: color 0.24s ease;
}

.contact-link.static {
  cursor: default;
}

.contact-link i {
  color: #6f7540;
}

.footer-links {
  display: grid;
  gap: 12px;
}

.footer-links a {
  color: #4f583f;
  text-decoration: none;
  transition: color 0.24s ease;
}

.footer-links a:hover,
.footer-links a:focus-visible,
.contact-link:hover,
.contact-link:focus-visible {
  color: #5f6737;
}

@media (max-width: 1080px) {
  .footer-inner {
    grid-template-columns: 1fr;
  }

  .footer-links {
    grid-template-columns: repeat(2, minmax(0, max-content));
    gap: 12px 22px;
  }
}

@media (max-width: 960px) {
  .footer-inner {
    width: min(100%, calc(100% - 24px));
    padding: 22px 18px;
  }

  .contact-link {
    align-items: flex-start;
  }

  .footer-links {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .storefront-footer {
    padding-bottom: 18px;
  }

  .footer-inner {
    width: min(100%, calc(100% - 18px));
    gap: 20px;
    padding: 20px 16px;
    border-radius: 22px;
  }

  .contact-list,
  .footer-links {
    gap: 10px;
  }
}
</style>
