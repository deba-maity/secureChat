"use client";

import {
  type FormEvent,
  type ReactNode,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  ArchiveRestore,
  Bell,
  Check,
  CheckCheck,
  ChevronLeft,
  Download,
  KeyRound,
  LockKeyhole,
  LogOut,
  Moon,
  Search,
  Send,
  Settings as SettingsIcon,
  Shield,
  ShieldAlert,
  Smile,
  Star,
  TimerReset,
  Trash2,
  Upload,
  UserPlus,
  X,
} from "lucide-react";
import { ApiError, api } from "@/lib/api";
import { cn, formatRelative, formatTime, initials } from "@/lib/utils";
import type {
  ChatMessage,
  Conversation,
  FavoriteBackup,
  Settings,
  UserSummary,
} from "@/lib/types";
import { useSecureSocket } from "@/hooks/use-secure-socket";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Label } from "@/components/ui/label";

type Session = {
  token: string;
  user: UserSummary;
};

const SESSION_KEY = "privac.session";
const EMOJIS = ["😀", "😎", "❤️", "🔥", "👍", "🎉", "🔒"];
const TIMER_OPTIONS = [
  { label: "Off", value: 0 },
  { label: "30 sec", value: 30 },
  { label: "1 min", value: 60 },
  { label: "1 hour", value: 3600 },
  { label: "1 day", value: 86400 },
];
const MESSAGE_POLL_INTERVAL_MS = 1000;

function tempCacheKey(userId: string, conversationId: string) {
  return `privac.temp.${userId}.${conversationId}`;
}

function upsertMessage(messages: ChatMessage[], message: ChatMessage) {
  const next = new Map(messages.map((item) => [item.id, item]));
  next.set(message.id, message);
  return Array.from(next.values()).sort(
    (first, second) =>
      new Date(first.createdAt).getTime() -
      new Date(second.createdAt).getTime(),
  );
}

function sameMessageSnapshot(first: ChatMessage[], second: ChatMessage[]) {
  return (
    first.length === second.length &&
    first.every((message, index) => {
      const next = second[index];
      return (
        next &&
        message.id === next.id &&
        message.content === next.content &&
        message.seenAt === next.seenAt &&
        message.selfDestructAt === next.selfDestructAt
      );
    })
  );
}

export function SecureChatApp() {
  const [mounted, setMounted] = useState(false);
  const [session, setSession] = useState<Session | null>(null);
  const [settings, setSettings] = useState<Settings | null>(null);
  const [favorites, setFavorites] = useState<Conversation[]>([]);
  const [activeConversation, setActiveConversation] =
    useState<Conversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [messageSearch, setMessageSearch] = useState("");
  const [messageSearchResults, setMessageSearchResults] = useState<
    ChatMessage[]
  >([]);
  const [draft, setDraft] = useState("");
  const [selfDestructSeconds, setSelfDestructSeconds] = useState(0);
  const [notice, setNotice] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [typingUser, setTypingUser] = useState<string | null>(null);
  const [unlockedFavoriteIds, setUnlockedFavoriteIds] = useState<Set<string>>(
    new Set(),
  );
  const [pinDraft, setPinDraft] = useState("");
  const [favoritePin, setFavoritePin] = useState("");
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const activeRef = useRef<Conversation | null>(null);
  const token = session?.token;

  useEffect(() => {
    setMounted(true);
    const raw =
      window.sessionStorage.getItem(SESSION_KEY) ??
      window.localStorage.getItem(SESSION_KEY);
    if (raw) {
      setSession(JSON.parse(raw) as Session);
      window.sessionStorage.setItem(SESSION_KEY, raw);
      window.localStorage.removeItem(SESSION_KEY);
    }
  }, []);

  useEffect(() => {
    activeRef.current = activeConversation;
  }, [activeConversation]);

  useEffect(() => {
    document.documentElement.classList.toggle(
      "dark",
      settings?.darkMode ?? true,
    );
  }, [settings?.darkMode]);

  const persistSession = useCallback((nextSession: Session | null) => {
    setSession(nextSession);
    if (nextSession) {
      window.sessionStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
      window.localStorage.removeItem(SESSION_KEY);
    } else {
      window.sessionStorage.removeItem(SESSION_KEY);
      window.localStorage.removeItem(SESSION_KEY);
    }
  }, []);

  const showNotice = useCallback((message: string) => {
    setNotice(message);
    window.setTimeout(() => setNotice(null), 3200);
  }, []);

  const loadFavorites = useCallback(async () => {
    if (!token) return;
    const data = await api.favorites(token);
    setFavorites(data);
  }, [token]);

  const loadSettings = useCallback(async () => {
    if (!token) return;
    const data = await api.settings(token);
    setSettings(data);
  }, [token]);

  useEffect(() => {
    if (!token) return;
    void Promise.all([loadFavorites(), loadSettings()]).catch((error) =>
      showNotice(
        error instanceof Error ? error.message : "Unable to load account",
      ),
    );
  }, [loadFavorites, loadSettings, showNotice, token]);

  useEffect(() => {
    if (!token || searchQuery.trim().length < 2) {
      setSearchResults([]);
      return;
    }
    const handle = window.setTimeout(() => {
      void api
        .searchUsers(token, searchQuery.trim())
        .then(setSearchResults)
        .catch((error) =>
          showNotice(error instanceof Error ? error.message : "Search failed"),
        );
    }, 220);
    return () => window.clearTimeout(handle);
  }, [searchQuery, showNotice, token]);

  useEffect(() => {
    if (
      !token ||
      !activeConversation?.favorite ||
      messageSearch.trim().length < 2
    ) {
      setMessageSearchResults([]);
      return;
    }
    const handle = window.setTimeout(() => {
      void api
        .searchMessages(token, activeConversation.id, messageSearch.trim())
        .then(setMessageSearchResults)
        .catch((error) =>
          showNotice(
            error instanceof Error ? error.message : "Message search failed",
          ),
        );
    }, 220);
    return () => window.clearTimeout(handle);
  }, [
    activeConversation?.favorite,
    activeConversation?.id,
    messageSearch,
    showNotice,
    token,
  ]);

  useEffect(() => {
    if (!session?.user.id || !activeConversation || activeConversation.favorite)
      return;
    window.localStorage.setItem(
      tempCacheKey(session.user.id, activeConversation.id),
      JSON.stringify(messages),
    );
  }, [activeConversation, messages, session?.user.id]);

  const notifyIncoming = useCallback((message: ChatMessage) => {
    if (
      typeof Notification === "undefined" ||
      Notification.permission !== "granted"
    )
      return;
    new Notification(message.sender.displayName, {
      body: message.content,
      icon: "/icon.svg",
    });
  }, []);

  const handleSocketMessage = useCallback(
    (message: ChatMessage) => {
      const current = activeRef.current;
      if (current?.id === message.conversationId) {
        setMessages((existing) => upsertMessage(existing, message));
        if (token && message.recipient.id === session?.user.id) {
          void api.markSeen(token, message.conversationId);
        }
      } else if (message.recipient.id === session?.user.id) {
        notifyIncoming(message);
      }
      void loadFavorites().catch(() => undefined);
    },
    [loadFavorites, notifyIncoming, session?.user.id, token],
  );

  const { connected, sendTyping } = useSecureSocket({
    token,
    onMessage: handleSocketMessage,
    onTyping: (event) => {
      if (activeRef.current?.id !== event.conversationId) return;
      setTypingUser(event.typing ? event.username : null);
      if (event.typing) {
        window.setTimeout(() => setTypingUser(null), 2200);
      }
    },
  });
  const activeConversationId = activeConversation?.id;
  const activeConversationLocked = activeConversation?.locked ?? false;
  const activeConversationUnlocked = activeConversationId
    ? unlockedFavoriteIds.has(activeConversationId)
    : false;

  useEffect(() => {
    if (!token || !session?.user.id || !activeConversationId) return;
    if (activeConversationLocked && !activeConversationUnlocked) return;

    let cancelled = false;
    let inFlight = false;
    const currentUserId = session.user.id;

    const pollMessages = async () => {
      if (inFlight) return;
      inFlight = true;
      try {
        const latestMessages = await api.messages(token, activeConversationId);
        if (cancelled || activeRef.current?.id !== activeConversationId) return;

        setMessages((existing) =>
          sameMessageSnapshot(existing, latestMessages)
            ? existing
            : latestMessages,
        );

        if (
          latestMessages.some(
            (message) =>
              message.recipient.id === currentUserId && !message.seenAt,
          )
        ) {
          void api.markSeen(token, activeConversationId).catch(() => undefined);
        }
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) {
          window.localStorage.removeItem(
            tempCacheKey(currentUserId, activeConversationId),
          );
          setActiveConversation(null);
          setMessages([]);
          setMessageSearch("");
          setMessageSearchResults([]);
          showNotice("Temporary chat cleared");
          return;
        }
      } finally {
        inFlight = false;
      }
    };

    void pollMessages();
    const interval = window.setInterval(
      () => void pollMessages(),
      MESSAGE_POLL_INTERVAL_MS,
    );

    return () => {
      cancelled = true;
      window.clearInterval(interval);
    };
  }, [
    activeConversationId,
    activeConversationLocked,
    activeConversationUnlocked,
    session?.user.id,
    showNotice,
    token,
  ]);

  const closeActiveConversation = useCallback(
    async (silent = false) => {
      if (!token || !session?.user.id || !activeRef.current) return;
      const closing = activeRef.current;
      if (!closing.favorite) {
        await api.deleteTemporary(token, closing.id).catch(() => undefined);
        window.localStorage.removeItem(
          tempCacheKey(session.user.id, closing.id),
        );
      }
      setActiveConversation(null);
      setMessages([]);
      setMessageSearch("");
      setMessageSearchResults([]);
      setUnlockedFavoriteIds(new Set());
      if (!silent) {
        await loadFavorites().catch(() => undefined);
      }
    },
    [loadFavorites, session?.user.id, token],
  );

  const openConversation = useCallback(
    async (conversation: Conversation) => {
      if (!token || !session?.user.id) return;
      if (activeRef.current?.id && activeRef.current.id !== conversation.id) {
        await closeActiveConversation(true);
      }
      setActiveConversation(conversation);
      if (conversation.locked && !unlockedFavoriteIds.has(conversation.id)) {
        setMessages([]);
        return;
      }
      const serverMessages = await api.messages(token, conversation.id);
      const cached = !conversation.favorite
        ? JSON.parse(
            window.localStorage.getItem(
              tempCacheKey(session.user.id, conversation.id),
            ) ?? "[]",
          )
        : [];
      setMessages(serverMessages.length > 0 ? serverMessages : cached);
      await api.markSeen(token, conversation.id).catch(() => undefined);
      await loadFavorites().catch(() => undefined);
    },
    [
      closeActiveConversation,
      loadFavorites,
      session?.user.id,
      token,
      unlockedFavoriteIds,
    ],
  );

  const startWithUser = useCallback(
    async (user: UserSummary) => {
      if (!token) return;
      const conversation = await api.startConversation(token, user.id);
      setSearchQuery("");
      setSearchResults([]);
      await openConversation(conversation);
    },
    [openConversation, token],
  );

  const handleFavoriteToggle = useCallback(async () => {
    if (!token || !activeConversation) return;
    const next = activeConversation.favorite
      ? await api.unfavorite(token, activeConversation.id)
      : await api.favorite(token, activeConversation.id);
    setActiveConversation(next);
    if (next.favorite) {
      setUnlockedFavoriteIds((existing) => new Set(existing).add(next.id));
    }
    await loadFavorites();
  }, [activeConversation, loadFavorites, token]);

  const handleSend = useCallback(async () => {
    if (!token || !activeConversation || !draft.trim()) return;
    const content = draft.trim();
    const payload = {
      conversationId: activeConversation.id,
      recipientId: activeConversation.participant.id,
      content,
      selfDestructSeconds,
    };
    setDraft("");
    sendTyping(activeConversation.id, activeConversation.participant.id, false);

    try {
      const saved = await api.sendMessage(token, payload);
      setMessages((existing) => upsertMessage(existing, saved));
      await loadFavorites().catch(() => undefined);
    } catch (error) {
      setDraft((current) => current || content);
      showNotice(
        error instanceof Error ? error.message : "Message failed to send",
      );
    }
  }, [
    activeConversation,
    draft,
    loadFavorites,
    selfDestructSeconds,
    sendTyping,
    showNotice,
    token,
  ]);

  const updateSetting = useCallback(
    async (payload: Partial<Settings> & { favoritePin?: string }) => {
      if (!token) return;
      const next = await api.updateSettings(token, payload);
      setSettings(next);
      await loadFavorites().catch(() => undefined);
    },
    [loadFavorites, token],
  );

  const unlockFavorite = useCallback(async () => {
    if (!token || !activeConversation) return;
    const result = await api.verifyFavoritePin(token, pinDraft);
    if (!result.valid) {
      showNotice("PIN did not match");
      return;
    }
    setUnlockedFavoriteIds((existing) =>
      new Set(existing).add(activeConversation.id),
    );
    setPinDraft("");
    const serverMessages = await api.messages(token, activeConversation.id);
    setMessages(serverMessages);
  }, [activeConversation, pinDraft, showNotice, token]);

  const handleExport = useCallback(async () => {
    if (!token) return;
    const backup = await api.exportFavorites(token);
    downloadJson(
      backup,
      `privac-favorites-${new Date().toISOString().slice(0, 10)}.json`,
    );
    showNotice("Encrypted favorite backup exported");
  }, [showNotice, token]);

  const handleImport = useCallback(
    async (file?: File) => {
      if (!token || !file) return;
      const backup = JSON.parse(await file.text()) as FavoriteBackup;
      const result = await api.importFavorites(token, backup);
      await loadFavorites();
      showNotice(`${result.messagesImported} encrypted messages imported`);
    },
    [loadFavorites, showNotice, token],
  );

  const handleLogout = useCallback(async () => {
    if (token) {
      await closeActiveConversation(true);
      await api.logout(token).catch(() => undefined);
    }
    persistSession(null);
    setSettings(null);
    setFavorites([]);
    setMessages([]);
    setActiveConversation(null);
  }, [closeActiveConversation, persistSession, token]);

  if (!mounted) {
    return <div className="min-h-screen bg-background" />;
  }

  if (!session) {
    return <AuthScreen onAuthenticated={persistSession} />;
  }

  const locked =
    activeConversation?.locked &&
    !unlockedFavoriteIds.has(activeConversation.id);

  return (
    <main className="privacy-grid min-h-screen overflow-hidden bg-background">
      <div className="mx-auto flex min-h-screen w-full max-w-[1540px] flex-col gap-4 p-3 md:p-5">
        <header className="glass-panel flex min-h-16 items-center justify-between rounded-lg px-3 py-2 md:px-4">
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary text-sm font-black text-primary-foreground">
              P
            </div>
            <div className="min-w-0">
              <h1 className="truncate text-lg font-black">Privac</h1>
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <span
                  className={cn(
                    "h-2 w-2 rounded-full",
                    connected ? "bg-primary" : "bg-secondary",
                  )}
                />
                <span>{connected ? "Realtime" : "Offline queue"}</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {settings?.screenshotWarningEnabled && (
              <Button
                variant="outline"
                size="icon"
                title="Screenshot warning demo active"
              >
                <ShieldAlert className="h-4 w-4 text-secondary" />
              </Button>
            )}
            <Button
              variant="outline"
              size="icon"
              title="Desktop notifications"
              onClick={() => {
                if (typeof Notification === "undefined") {
                  showNotice("Desktop notifications are unavailable");
                  return;
                }
                void Notification.requestPermission().then((permission) =>
                  showNotice(
                    permission === "granted"
                      ? "Notifications enabled"
                      : "Notifications blocked",
                  ),
                );
              }}
            >
              <Bell className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="icon"
              title="Settings"
              onClick={() => setSettingsOpen((open) => !open)}
            >
              <SettingsIcon className="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              title="Logout"
              onClick={() => void handleLogout()}
            >
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </header>

        <div
          className={cn(
            "grid min-h-0 flex-1 gap-4 lg:grid-cols-[360px_minmax(0,1fr)]",
            settingsOpen && "xl:grid-cols-[380px_minmax(0,1fr)_360px]",
          )}
        >
          <aside className="glass-panel flex min-h-[420px] flex-col rounded-lg">
            <div className="border-b border-border p-4">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-sm font-bold uppercase text-muted-foreground">
                  Home
                </h2>
                <Badge variant="outline">No default list</Badge>
              </div>
              <div className="relative">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  className="pl-9"
                  value={searchQuery}
                  onChange={(event) => setSearchQuery(event.target.value)}
                  placeholder="Username or phone"
                />
              </div>
              <div className="mt-3 space-y-2">
                {searchResults.map((user) => (
                  <UserRow
                    key={user.id}
                    user={user}
                    trailing={<UserPlus className="h-4 w-4" />}
                    onClick={() => void startWithUser(user)}
                  />
                ))}
              </div>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto p-4">
              <div className="mb-3 flex items-center justify-between">
                <h2 className="text-sm font-bold uppercase text-muted-foreground">
                  Favorites
                </h2>
                <Badge>{favorites.length}</Badge>
              </div>
              <div className="space-y-2">
                {favorites.map((conversation) => (
                  <ConversationRow
                    key={conversation.id}
                    conversation={conversation}
                    active={activeConversation?.id === conversation.id}
                    onClick={() => void openConversation(conversation)}
                  />
                ))}
                {favorites.length === 0 && (
                  <EmptyState
                    icon={<Star className="h-5 w-5" />}
                    title="No favorites yet"
                  />
                )}
              </div>
            </div>

            <div className="border-t border-border p-4">
              <UserRow
                user={session.user}
                trailing={<Shield className="h-4 w-4 text-primary" />}
                onClick={() => setSettingsOpen(true)}
              />
            </div>
          </aside>

          <section className="glass-panel flex min-h-[620px] flex-col overflow-hidden rounded-lg">
            {!activeConversation ? (
              <div className="flex flex-1 items-center justify-center p-6">
                <EmptyState
                  icon={<Search className="h-6 w-6" />}
                  title="No conversation open"
                />
              </div>
            ) : (
              <>
                <ChatHeader
                  conversation={activeConversation}
                  onClose={() => void closeActiveConversation()}
                  onFavoriteToggle={() => void handleFavoriteToggle()}
                />

                {activeConversation.favorite && !locked && (
                  <div className="border-b border-border px-4 py-3">
                    <div className="relative">
                      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                      <Input
                        className="pl-9"
                        value={messageSearch}
                        onChange={(event) =>
                          setMessageSearch(event.target.value)
                        }
                        placeholder="Search favorite messages"
                      />
                    </div>
                    {messageSearchResults.length > 0 && (
                      <div className="mt-2 flex gap-2 overflow-x-auto pb-1">
                        {messageSearchResults.map((message) => (
                          <button
                            key={message.id}
                            className="max-w-[260px] shrink-0 truncate rounded-lg border border-border bg-background/80 px-3 py-2 text-left text-xs"
                            onClick={() => setMessageSearch("")}
                          >
                            {message.content}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {locked ? (
                  <div className="flex flex-1 items-center justify-center p-6">
                    <form
                      className="w-full max-w-sm rounded-lg border border-border bg-background/80 p-4"
                      onSubmit={(event) => {
                        event.preventDefault();
                        void unlockFavorite();
                      }}
                    >
                      <div className="mb-4 flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/12 text-primary">
                          <LockKeyhole className="h-5 w-5" />
                        </div>
                        <div>
                          <h2 className="font-bold">Favorite locked</h2>
                          <p className="text-sm text-muted-foreground">
                            {activeConversation.participant.displayName}
                          </p>
                        </div>
                      </div>
                      <Input
                        value={pinDraft}
                        onChange={(event) => setPinDraft(event.target.value)}
                        type="password"
                        inputMode="numeric"
                        placeholder="PIN"
                      />
                      <Button className="mt-3 w-full" type="submit">
                        <KeyRound className="h-4 w-4" />
                        Unlock
                      </Button>
                    </form>
                  </div>
                ) : (
                  <>
                    <MessageList
                      messages={messages}
                      currentUserId={session.user.id}
                      typingUser={typingUser}
                    />
                    <Composer
                      draft={draft}
                      selfDestructSeconds={selfDestructSeconds}
                      onDraftChange={(value) => {
                        setDraft(value);
                        if (activeConversation) {
                          sendTyping(
                            activeConversation.id,
                            activeConversation.participant.id,
                            value.length > 0,
                          );
                        }
                      }}
                      onEmoji={(emoji) =>
                        setDraft((value) => `${value}${emoji}`)
                      }
                      onTimerChange={setSelfDestructSeconds}
                      onSend={() => void handleSend()}
                    />
                  </>
                )}
              </>
            )}
          </section>

          <aside
            className={cn(
              "glass-panel min-h-[620px] rounded-lg",
              settingsOpen ? "block" : "hidden",
            )}
          >
            {settings && (
              <SettingsPanel
                settings={settings}
                favoritePin={favoritePin}
                onFavoritePinChange={setFavoritePin}
                onToggle={(key, value) => void updateSetting({ [key]: value })}
                onFavoriteLockChange={(value) => {
                  if (value && !settings.favoritePinConfigured) {
                    showNotice("Set a favorite chat PIN first");
                    return;
                  }
                  void updateSetting({ lockFavoriteChats: value });
                }}
                onSetPin={() => {
                  if (favoritePin.trim().length < 4) {
                    showNotice("PIN must be at least 4 digits");
                    return;
                  }
                  void updateSetting({ favoritePin })
                    .then(() => {
                      setFavoritePin("");
                      showNotice("Favorite chat lock enabled");
                    })
                    .catch((error) =>
                      showNotice(
                        error instanceof Error
                          ? error.message
                          : "Unable to set PIN",
                      ),
                    );
                }}
                onExport={() => void handleExport()}
                onImport={() => fileInputRef.current?.click()}
                onDeleteAccount={() => {
                  if (
                    !token ||
                    !window.confirm("Delete your account and all stored data?")
                  )
                    return;
                  void api
                    .deleteAccount(token)
                    .then(() => persistSession(null));
                }}
              />
            )}
          </aside>
        </div>
      </div>

      <input
        ref={fileInputRef}
        className="hidden"
        type="file"
        accept="application/json"
        onChange={(event) => void handleImport(event.target.files?.[0])}
      />

      {notice && (
        <div className="fixed bottom-4 left-1/2 z-50 max-w-[92vw] -translate-x-1/2 rounded-lg border border-border bg-card px-4 py-3 text-sm shadow-glow animate-in fade-in slide-in-from-bottom-2">
          {notice}
        </div>
      )}
    </main>
  );
}

function AuthScreen({
  onAuthenticated,
}: {
  onAuthenticated: (session: Session) => void;
}) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState({
    username: "",
    usernameOrPhone: "",
    phoneNumber: "",
    displayName: "",
    password: "",
  });

  useEffect(() => {
    document.documentElement.classList.add("dark");
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response =
        mode === "login"
          ? await api.login({
              usernameOrPhone: form.usernameOrPhone,
              password: form.password,
            })
          : await api.register({
              username: form.username,
              phoneNumber: form.phoneNumber,
              displayName: form.displayName,
              password: form.password,
            });
      onAuthenticated(response);
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Authentication failed",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="min-h-screen bg-background p-3 text-foreground md:p-6">
      <div className="mx-auto grid min-h-[calc(100vh-3rem)] max-w-6xl overflow-hidden rounded-lg border border-white/10 bg-card shadow-panel lg:grid-cols-[1.1fr_0.9fr]">
        <div className="relative hidden min-h-[680px] overflow-hidden lg:block">
          <img
            src="/privacy-visual.png"
            alt="Encrypted glass lock visual"
            className="h-full w-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-background/35 via-transparent to-background/70" />
          <div className="absolute bottom-8 left-8 max-w-md">
            <Badge variant="warning">Temporary by default</Badge>
            <h1 className="mt-4 text-5xl font-black leading-tight">Privac</h1>
            <p className="mt-3 text-base text-foreground/78">
              Search-first messaging with encrypted favorites and chats that
              vanish when you leave.
            </p>
          </div>
        </div>

        <div className="flex items-center justify-center p-5 md:p-10">
          <form
            className="w-full max-w-md animate-slide-up"
            onSubmit={handleSubmit}
          >
            <div className="mb-8">
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-lg bg-primary text-lg font-black text-primary-foreground">
                P
              </div>
              <h2 className="text-3xl font-black">
                {mode === "login" ? "Welcome back" : "Create account"}
              </h2>
              <p className="mt-2 text-sm text-muted-foreground">
                Secure chat, no permanent trail by default.
              </p>
            </div>

            <div className="space-y-3">
              {mode === "register" ? (
                <>
                  <Input
                    required
                    value={form.displayName}
                    onChange={(event) =>
                      setForm((value) => ({
                        ...value,
                        displayName: event.target.value,
                      }))
                    }
                    placeholder="Display name"
                  />
                  <Input
                    required
                    value={form.username}
                    onChange={(event) =>
                      setForm((value) => ({
                        ...value,
                        username: event.target.value,
                      }))
                    }
                    placeholder="Username"
                  />
                  <Input
                    required
                    value={form.phoneNumber}
                    onChange={(event) =>
                      setForm((value) => ({
                        ...value,
                        phoneNumber: event.target.value,
                      }))
                    }
                    placeholder="Phone number"
                  />
                </>
              ) : (
                <Input
                  required
                  value={form.usernameOrPhone}
                  onChange={(event) =>
                    setForm((value) => ({
                      ...value,
                      usernameOrPhone: event.target.value,
                    }))
                  }
                  placeholder="Username or phone"
                />
              )}
              <Input
                required
                minLength={8}
                type="password"
                value={form.password}
                onChange={(event) =>
                  setForm((value) => ({
                    ...value,
                    password: event.target.value,
                  }))
                }
                placeholder="Password"
              />
            </div>

            {error && (
              <p className="mt-3 text-sm font-medium text-destructive">
                {error}
              </p>
            )}

            <Button className="mt-5 w-full" type="submit" disabled={loading}>
              {loading
                ? "Please wait"
                : mode === "login"
                  ? "Login"
                  : "Register"}
            </Button>
            <Button
              className="mt-3 w-full"
              type="button"
              variant="ghost"
              onClick={() =>
                setMode((value) => (value === "login" ? "register" : "login"))
              }
            >
              {mode === "login" ? "Create account" : "Use existing account"}
            </Button>
            {mode === "login" && (
              <Button
                className="mt-2 w-full"
                type="button"
                variant="ghost"
                onClick={() =>
                  setError(
                    "Password reset needs verified email or SMS and is not enabled in this local build.",
                  )
                }
              >
                Forgot password?
              </Button>
            )}
          </form>
        </div>
      </div>
    </main>
  );
}

function UserRow({
  user,
  trailing,
  onClick,
}: {
  user: UserSummary;
  trailing?: ReactNode;
  onClick: () => void;
}) {
  return (
    <button
      className="flex w-full items-center gap-3 rounded-lg border border-transparent px-2 py-2 text-left transition hover:border-border hover:bg-background/70"
      onClick={onClick}
    >
      <Avatar user={user} />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-semibold">{user.displayName}</p>
        <p className="truncate text-xs text-muted-foreground">
          @{user.username}
        </p>
      </div>
      <div className="flex items-center gap-2">
        <span
          className={cn(
            "h-2 w-2 rounded-full",
            user.online ? "bg-primary" : "bg-muted-foreground/40",
          )}
        />
        {trailing}
      </div>
    </button>
  );
}

function ConversationRow({
  conversation,
  active,
  onClick,
}: {
  conversation: Conversation;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      className={cn(
        "flex w-full items-center gap-3 rounded-lg border px-2 py-2 text-left transition",
        active
          ? "border-primary bg-primary/10"
          : "border-transparent hover:border-border hover:bg-background/70",
      )}
      onClick={onClick}
    >
      <Avatar user={conversation.participant} />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="truncate text-sm font-semibold">
            {conversation.participant.displayName}
          </p>
          {conversation.locked && (
            <LockKeyhole className="h-3.5 w-3.5 text-accent" />
          )}
        </div>
        <p className="truncate text-xs text-muted-foreground">
          {conversation.participant.online
            ? "Online"
            : formatRelative(conversation.participant.lastSeen)}
        </p>
      </div>
      <div className="flex flex-col items-end gap-1">
        <Star className="h-4 w-4 fill-accent text-accent" />
        {conversation.unreadCount > 0 && (
          <Badge>{conversation.unreadCount}</Badge>
        )}
      </div>
    </button>
  );
}

function ChatHeader({
  conversation,
  onClose,
  onFavoriteToggle,
}: {
  conversation: Conversation;
  onClose: () => void;
  onFavoriteToggle: () => void;
}) {
  return (
    <div className="flex min-h-16 items-center justify-between border-b border-border px-3 py-2 md:px-4">
      <div className="flex min-w-0 items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          title="Leave chat"
          onClick={onClose}
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <Avatar user={conversation.participant} />
        <div className="min-w-0">
          <h2 className="truncate text-base font-bold">
            {conversation.participant.displayName}
          </h2>
          <p className="truncate text-xs text-muted-foreground">
            {conversation.participant.online
              ? "Online"
              : formatRelative(conversation.participant.lastSeen)}
          </p>
        </div>
      </div>
      <div className="flex items-center gap-2">
        {conversation.favorite ? (
          <Badge variant="warning">Favorite</Badge>
        ) : (
          <Badge variant="outline">Temporary</Badge>
        )}
        <Button
          variant={conversation.favorite ? "secondary" : "outline"}
          size="icon"
          title={conversation.favorite ? "Remove favorite" : "Favorite chat"}
          onClick={onFavoriteToggle}
        >
          <Star
            className={cn("h-4 w-4", conversation.favorite && "fill-current")}
          />
        </Button>
        <Button variant="ghost" size="icon" title="Close" onClick={onClose}>
          <X className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}

function MessageList({
  messages,
  currentUserId,
  typingUser,
}: {
  messages: ChatMessage[];
  currentUserId: string;
  typingUser: string | null;
}) {
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages.length, typingUser]);

  return (
    <div className="min-h-0 flex-1 overflow-y-auto px-3 py-4 md:px-6">
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-3">
        {messages.length === 0 && (
          <EmptyState
            icon={<Shield className="h-5 w-5" />}
            title="Encrypted session ready"
          />
        )}
        {messages.map((message) => {
          const mine = message.sender.id === currentUserId;
          return (
            <div
              key={message.id}
              className={cn("flex", mine ? "justify-end" : "justify-start")}
            >
              <div
                className={cn(
                  "max-w-[82%] rounded-lg px-3 py-2 shadow-sm md:max-w-[68%]",
                  mine
                    ? "bg-primary text-primary-foreground"
                    : "border border-border bg-background/85 text-foreground",
                )}
              >
                <p className="whitespace-pre-wrap break-words text-sm leading-relaxed">
                  {message.content}
                </p>
                <div
                  className={cn(
                    "mt-1 flex items-center justify-end gap-1 text-[11px]",
                    mine
                      ? "text-primary-foreground/75"
                      : "text-muted-foreground",
                  )}
                >
                  {message.selfDestructAt && <TimerReset className="h-3 w-3" />}
                  <span>{formatTime(message.createdAt)}</span>
                  {mine &&
                    (message.seenAt ? (
                      <CheckCheck className="h-3 w-3" />
                    ) : (
                      <Check className="h-3 w-3" />
                    ))}
                </div>
              </div>
            </div>
          );
        })}
        {typingUser && (
          <div className="w-fit rounded-lg border border-border bg-background/80 px-3 py-2 text-xs text-muted-foreground">
            {typingUser} typing
          </div>
        )}
        <div ref={bottomRef} />
      </div>
    </div>
  );
}

function Composer({
  draft,
  selfDestructSeconds,
  onDraftChange,
  onEmoji,
  onTimerChange,
  onSend,
}: {
  draft: string;
  selfDestructSeconds: number;
  onDraftChange: (value: string) => void;
  onEmoji: (emoji: string) => void;
  onTimerChange: (value: number) => void;
  onSend: () => void;
}) {
  const timerLabel = useMemo(
    () =>
      TIMER_OPTIONS.find((option) => option.value === selfDestructSeconds)
        ?.label ?? "Off",
    [selfDestructSeconds],
  );

  return (
    <div className="border-t border-border bg-background/50 p-3 md:p-4">
      <div className="mx-auto w-full max-w-3xl">
        <div className="mb-2 flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-1 rounded-lg border border-border bg-background/80 p-1">
            <Smile className="ml-2 h-4 w-4 text-muted-foreground" />
            {EMOJIS.map((emoji) => (
              <button
                key={emoji}
                className="flex h-7 w-7 items-center justify-center rounded-md text-sm hover:bg-muted"
                type="button"
                onClick={() => onEmoji(emoji)}
              >
                {emoji}
              </button>
            ))}
          </div>
          <label className="flex h-9 items-center gap-2 rounded-lg border border-border bg-background/80 px-2 text-xs">
            <TimerReset className="h-4 w-4 text-muted-foreground" />
            <select
              className="bg-transparent text-xs outline-none"
              value={selfDestructSeconds}
              onChange={(event) => onTimerChange(Number(event.target.value))}
              title={`Self-destruct timer: ${timerLabel}`}
            >
              {TIMER_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>
        <div className="flex items-end gap-2">
          <Textarea
            className="min-h-12 resize-none"
            value={draft}
            onChange={(event) => onDraftChange(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                onSend();
              }
            }}
            placeholder="Message"
          />
          <Button
            size="icon"
            title="Send"
            onClick={onSend}
            disabled={!draft.trim()}
          >
            <Send className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}

function SettingsPanel({
  settings,
  favoritePin,
  onFavoritePinChange,
  onToggle,
  onFavoriteLockChange,
  onSetPin,
  onExport,
  onImport,
  onDeleteAccount,
}: {
  settings: Settings;
  favoritePin: string;
  onFavoritePinChange: (value: string) => void;
  onToggle: (key: keyof Settings, value: boolean) => void;
  onFavoriteLockChange: (value: boolean) => void;
  onSetPin: () => void;
  onExport: () => void;
  onImport: () => void;
  onDeleteAccount: () => void;
}) {
  return (
    <div className="flex h-full flex-col">
      <div className="border-b border-border p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/12 text-primary">
            <SettingsIcon className="h-5 w-5" />
          </div>
          <div>
            <h2 className="font-black">Settings</h2>
            <p className="text-xs text-muted-foreground">Privacy controls</p>
          </div>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        <div className="space-y-3">
          <SettingToggle
            label="Dark Mode"
            icon={<Moon className="h-4 w-4" />}
            checked={settings.darkMode}
            onCheckedChange={(value) => onToggle("darkMode", value)}
          />
          <SettingToggle
            label="Auto Delete"
            icon={<Trash2 className="h-4 w-4" />}
            checked={settings.autoDeleteEnabled}
            onCheckedChange={(value) => onToggle("autoDeleteEnabled", value)}
          />
          <SettingToggle
            label="Hide Last Seen"
            icon={<Shield className="h-4 w-4" />}
            checked={settings.hideLastSeen}
            onCheckedChange={(value) => onToggle("hideLastSeen", value)}
          />
          <SettingToggle
            label="Hide Online Status"
            icon={<Shield className="h-4 w-4" />}
            checked={settings.hideOnlineStatus}
            onCheckedChange={(value) => onToggle("hideOnlineStatus", value)}
          />
          <SettingToggle
            label="Read Receipts"
            icon={<CheckCheck className="h-4 w-4" />}
            checked={settings.readReceiptsEnabled}
            onCheckedChange={(value) => onToggle("readReceiptsEnabled", value)}
          />
          <SettingToggle
            label="Screenshot Warning"
            icon={<ShieldAlert className="h-4 w-4" />}
            checked={settings.screenshotWarningEnabled}
            onCheckedChange={(value) =>
              onToggle("screenshotWarningEnabled", value)
            }
          />
        </div>

        <div className="mt-5 space-y-3 rounded-lg border border-border bg-background/70 p-3">
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <Label>Favorite chat lock</Label>
              <p className="mt-1 text-xs text-muted-foreground">
                Require a PIN before opening favorite conversations.
              </p>
            </div>
            <Switch
              checked={settings.lockFavoriteChats}
              onCheckedChange={onFavoriteLockChange}
            />
          </div>
          <div className="flex gap-2">
            <Input
              value={favoritePin}
              onChange={(event) => onFavoritePinChange(event.target.value)}
              type="password"
              inputMode="numeric"
              placeholder={
                settings.favoritePinConfigured
                  ? "Update PIN"
                  : "Create PIN first"
              }
            />
            <Button size="icon" title="Set PIN" onClick={onSetPin}>
              <KeyRound className="h-4 w-4" />
            </Button>
          </div>
        </div>

        <div className="mt-5 grid grid-cols-2 gap-2">
          <Button variant="outline" onClick={onExport}>
            <Download className="h-4 w-4" />
            Export
          </Button>
          <Button variant="outline" onClick={onImport}>
            <Upload className="h-4 w-4" />
            Import
          </Button>
        </div>

        <Button
          className="mt-5 w-full"
          variant="destructive"
          onClick={onDeleteAccount}
        >
          <Trash2 className="h-4 w-4" />
          Delete account
        </Button>
      </div>
    </div>
  );
}

function SettingToggle({
  label,
  icon,
  checked,
  onCheckedChange,
}: {
  label: string;
  icon: ReactNode;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-lg border border-border bg-background/70 px-3 py-2">
      <div className="flex min-w-0 items-center gap-2">
        <span className="text-muted-foreground">{icon}</span>
        <span className="truncate text-sm font-medium">{label}</span>
      </div>
      <Switch checked={checked} onCheckedChange={onCheckedChange} />
    </div>
  );
}

function Avatar({ user }: { user: UserSummary }) {
  return user.profilePictureUrl ? (
    <img
      src={user.profilePictureUrl}
      alt={user.displayName}
      className="h-10 w-10 rounded-lg object-cover"
    />
  ) : (
    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-muted text-sm font-black text-foreground">
      {initials(user.displayName || user.username)}
    </div>
  );
}

function EmptyState({ icon, title }: { icon: ReactNode; title: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-lg border border-dashed border-border bg-background/45 p-6 text-center text-muted-foreground">
      <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-muted text-foreground">
        {icon}
      </div>
      <p className="text-sm font-semibold">{title}</p>
    </div>
  );
}

function downloadJson(payload: unknown, filename: string) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], {
    type: "application/json",
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}
