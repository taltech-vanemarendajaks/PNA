import { NUMBER_WS_ROUTE} from "./requests";

let socket: WebSocket | null = null;

function toWebSocketUrl(baseUrl: string) {
    if (baseUrl.startsWith("https://")) {
        return baseUrl.replace("https://", "wss://");
    }

    if (baseUrl.startsWith("http://")) {
        return baseUrl.replace("http://", "ws://");
    }

    return baseUrl;
}

export function connectNumberSocket(onRefresh: () => void) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        return socket;
    }

    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL;
    const wsBaseUrl = toWebSocketUrl(apiBaseUrl);

    socket = new WebSocket(`${wsBaseUrl}${NUMBER_WS_ROUTE}`);

    socket.onopen = () => {
        console.log("number socket connected");
    };

    socket.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data) as { type?: string };

            if (data.type === "refreshCalls") {
                onRefresh();
            }
        } catch (error) {
            console.error("invalid websocket message", error);
        }
    };

    socket.onerror = (error) => {
        console.error("number socket error", error);
    };

    socket.onclose = (event) => {
        console.log("number socket disconnected", event.code, event.reason);
        socket = null;
    };

    return socket;
}

export function disconnectNumberSocket() {
    if (socket) {
        socket.close();
        socket = null;
    }
}
