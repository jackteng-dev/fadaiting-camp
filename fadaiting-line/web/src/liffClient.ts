import liff from "@line/liff";

export interface LiffProfileState {
  enabled: boolean;
  ready: boolean;
  loggedIn: boolean;
  userId: string;
  displayName: string;
  pictureUrl: string;
  errorMessage: string;
}

export async function initializeLiff(): Promise<LiffProfileState> {
  const liffId = import.meta.env.VITE_LIFF_ID || "";
  if (!liffId) {
    return {
      enabled: false,
      ready: false,
      loggedIn: false,
      userId: "",
      displayName: "",
      pictureUrl: "",
      errorMessage: "",
    };
  }

  try {
    await liff.init({ liffId });
    if (!liff.isLoggedIn()) {
      return {
        enabled: true,
        ready: true,
        loggedIn: false,
        userId: "",
        displayName: "",
        pictureUrl: "",
        errorMessage: "",
      };
    }

    const profile = await liff.getProfile();
    return {
      enabled: true,
      ready: true,
      loggedIn: true,
      userId: profile.userId,
      displayName: profile.displayName,
      pictureUrl: profile.pictureUrl || "",
      errorMessage: "",
    };
  } catch (error: any) {
    return {
      enabled: true,
      ready: false,
      loggedIn: false,
      userId: "",
      displayName: "",
      pictureUrl: "",
      errorMessage: error?.message || "LIFF 初始化失敗",
    };
  }
}

export function loginWithLiff() {
  liff.login();
}

export function logoutWithLiff() {
  liff.logout();
}
