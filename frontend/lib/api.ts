import type {
  AuthResponse,
  ChatMessage,
  Conversation,
  FavoriteBackup,
  ImportBackupResponse,
  SendMessagePayload,
  Settings,
  UserSummary
} from "@/lib/types";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type RequestOptions = RequestInit & {
  token?: string;
};

class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  const payload = contentType.includes("application/json") ? await response.json() : await response.text();

  if (!response.ok) {
    const message = typeof payload === "string" ? payload : payload.message ?? "Request failed";
    throw new ApiError(response.status, message);
  }

  return payload as T;
}

export const api = {
  register(payload: {
    username: string;
    phoneNumber: string;
    displayName: string;
    profilePictureUrl?: string;
    password: string;
  }) {
    return request<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },
  login(payload: { usernameOrPhone: string; password: string }) {
    return request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(payload)
    });
  },
  logout(token: string) {
    return request<void>("/api/auth/logout", { method: "POST", token });
  },
  changePassword(token: string, payload: { currentPassword: string; newPassword: string }) {
    return request<void>("/api/auth/password", {
      method: "POST",
      token,
      body: JSON.stringify(payload)
    });
  },
  me(token: string) {
    return request<UserSummary>("/api/users/me", { token });
  },
  searchUsers(token: string, query: string) {
    return request<UserSummary[]>(`/api/users/search?q=${encodeURIComponent(query)}`, { token });
  },
  startConversation(token: string, userId: string) {
    return request<Conversation>("/api/conversations/start", {
      method: "POST",
      token,
      body: JSON.stringify({ userId })
    });
  },
  favorites(token: string) {
    return request<Conversation[]>("/api/conversations/favorites", { token });
  },
  favorite(token: string, conversationId: string) {
    return request<Conversation>(`/api/conversations/${conversationId}/favorite`, {
      method: "POST",
      token
    });
  },
  unfavorite(token: string, conversationId: string) {
    return request<Conversation>(`/api/conversations/${conversationId}/favorite`, {
      method: "DELETE",
      token
    });
  },
  deleteTemporary(token: string, conversationId: string) {
    return request<void>(`/api/conversations/${conversationId}/temporary`, {
      method: "DELETE",
      token
    });
  },
  messages(token: string, conversationId: string) {
    return request<ChatMessage[]>(`/api/conversations/${conversationId}/messages`, { token });
  },
  markSeen(token: string, conversationId: string) {
    return request<void>(`/api/conversations/${conversationId}/seen`, {
      method: "POST",
      token
    });
  },
  sendMessage(token: string, payload: SendMessagePayload) {
    return request<ChatMessage>("/api/messages", {
      method: "POST",
      token,
      body: JSON.stringify(payload)
    });
  },
  searchMessages(token: string, conversationId: string, query: string) {
    return request<ChatMessage[]>(
      `/api/conversations/${conversationId}/messages/search?q=${encodeURIComponent(query)}`,
      { token }
    );
  },
  settings(token: string) {
    return request<Settings>("/api/settings", { token });
  },
  updateSettings(token: string, payload: Partial<Settings> & { favoritePin?: string }) {
    return request<Settings>("/api/settings", {
      method: "PUT",
      token,
      body: JSON.stringify(payload)
    });
  },
  exportFavorites(token: string) {
    return request<FavoriteBackup>("/api/settings/favorites/export", {
      method: "POST",
      token
    });
  },
  importFavorites(token: string, backup: FavoriteBackup) {
    return request<ImportBackupResponse>("/api/settings/favorites/import", {
      method: "POST",
      token,
      body: JSON.stringify(backup)
    });
  },
  verifyFavoritePin(token: string, pin: string) {
    return request<{ valid: boolean }>("/api/settings/favorite-pin/verify", {
      method: "POST",
      token,
      body: JSON.stringify({ pin })
    });
  },
  deleteAccount(token: string) {
    return request<void>("/api/settings/account", {
      method: "DELETE",
      token
    });
  }
};

export { ApiError };
