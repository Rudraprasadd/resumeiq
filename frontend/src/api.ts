export type UserInfo = {
  id: string;
  email: string;
  fullName: string;
  plan: "FREE" | "PRO" | string;
};

export type TokenResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: UserInfo;
};

export type RegisterPayload = {
  fullName: string;
  email: string;
  password: string;
};

export type LoginPayload = {
  email: string;
  password: string;
};

export type ApiError = {
  status?: number;
  error?: string;
  message?: string;
  path?: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init.headers
    }
  });

  const contentType = response.headers.get("content-type") ?? "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const error = body as ApiError | string;
    const message =
      typeof error === "string"
        ? error
        : error.message || error.error || "Request failed";
    throw new Error(message);
  }

  return body as T;
}

export const api = {
  register(payload: RegisterPayload) {
    return request<TokenResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  login(payload: LoginPayload) {
    return request<TokenResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },

  me(accessToken: string) {
    return request<UserInfo>("/api/auth/me", {
      headers: {
        Authorization: `Bearer ${accessToken}`
      }
    });
  },

  health() {
    return request<{ status: string }>("/actuator/health");
  }
};
