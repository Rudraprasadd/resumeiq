import type { TokenResponse, UserInfo } from "./api";

const AUTH_KEY = "resumeiq.auth";

export type AuthState = {
  accessToken: string;
  refreshToken: string;
  user: UserInfo;
};

export function loadAuth(): AuthState | null {
  const raw = localStorage.getItem(AUTH_KEY);
  if (!raw) return null;

  try {
    return JSON.parse(raw) as AuthState;
  } catch {
    localStorage.removeItem(AUTH_KEY);
    return null;
  }
}

export function saveAuth(response: TokenResponse): AuthState {
  const auth = {
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    user: response.user
  };
  localStorage.setItem(AUTH_KEY, JSON.stringify(auth));
  return auth;
}

export function clearAuth() {
  localStorage.removeItem(AUTH_KEY);
}
