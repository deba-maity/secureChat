"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_URL } from "@/lib/api";
import type { ChatMessage, SendMessagePayload } from "@/lib/types";

type TypingEvent = {
  conversationId: string;
  username: string;
  typing: boolean;
};

type UseSecureSocketOptions = {
  token?: string;
  onMessage: (message: ChatMessage) => void;
  onTyping: (event: TypingEvent) => void;
};

export function useSecureSocket({ token, onMessage, onTyping }: UseSecureSocketOptions) {
  const clientRef = useRef<Client | null>(null);
  const messageRef = useRef(onMessage);
  const typingRef = useRef(onTyping);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    messageRef.current = onMessage;
    typingRef.current = onTyping;
  }, [onMessage, onTyping]);

  useEffect(() => {
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_URL}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe("/user/queue/messages", (frame: IMessage) => {
          messageRef.current(JSON.parse(frame.body) as ChatMessage);
        });
        client.subscribe("/user/queue/typing", (frame: IMessage) => {
          typingRef.current(JSON.parse(frame.body) as TypingEvent);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      onWebSocketClose: () => setConnected(false)
    });

    client.activate();
    clientRef.current = client;

    return () => {
      clientRef.current = null;
      void client.deactivate();
      setConnected(false);
    };
  }, [token]);

  const sendMessage = useCallback((payload: SendMessagePayload) => {
    const client = clientRef.current;
    if (!client?.connected) return false;
    client.publish({
      destination: "/app/chat.send",
      body: JSON.stringify(payload)
    });
    return true;
  }, []);

  const sendTyping = useCallback((conversationId: string, recipientId: string, typing: boolean) => {
    const client = clientRef.current;
    if (!client?.connected) return;
    client.publish({
      destination: "/app/chat.typing",
      body: JSON.stringify({ conversationId, recipientId, typing })
    });
  }, []);

  return { connected, sendMessage, sendTyping };
}
