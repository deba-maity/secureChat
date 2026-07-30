export type UserSummary = {
  id: string;
  username: string;
  phoneNumber: string;
  displayName: string;
  profilePictureUrl?: string | null;
  online: boolean;
  lastSeen?: string | null;
};

export type AuthResponse = {
  token: string;
  user: UserSummary;
};

export type Conversation = {
  id: string;
  participant: UserSummary;
  favorite: boolean;
  locked: boolean;
  lastMessageAt?: string | null;
  unreadCount: number;
};

export type ChatMessage = {
  id: string;
  conversationId: string;
  sender: UserSummary;
  recipient: UserSummary;
  messageType: "TEXT";
  content: string;
  createdAt: string;
  deliveredAt?: string | null;
  seenAt?: string | null;
  selfDestructAt?: string | null;
  temporary: boolean;
};

export type Settings = {
  hideLastSeen: boolean;
  hideOnlineStatus: boolean;
  readReceiptsEnabled: boolean;
  screenshotWarningEnabled: boolean;
  lockFavoriteChats: boolean;
  darkMode: boolean;
  autoDeleteEnabled: boolean;
  favoritePinConfigured: boolean;
};

export type FavoriteBackup = {
  exportedAt: string;
  conversationCount: number;
  conversations: Array<{
    conversationId: string;
    participant: UserSummary;
    lastMessageAt?: string | null;
    messages: Array<{
      senderId: string;
      recipientId: string;
      messageType: "TEXT";
      iv: string;
      cipherText: string;
      createdAt: string;
      deliveredAt?: string | null;
      seenAt?: string | null;
      selfDestructAt?: string | null;
    }>;
  }>;
};

export type ImportBackupResponse = {
  conversationsImported: number;
  messagesImported: number;
};

export type SendMessagePayload = {
  conversationId?: string;
  recipientId?: string;
  content: string;
  selfDestructSeconds?: number;
};

