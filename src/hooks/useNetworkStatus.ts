"use client";

import { useState, useEffect, useCallback } from "react";

// Global in-memory simulation flag to ensure all components and services stay synchronized
let globalSimulatedOffline = false;
const listeners: Array<(offline: boolean) => void> = [];

export const getNetworkIsOnline = (): boolean => {
  if (globalSimulatedOffline) return false;
  if (typeof navigator !== "undefined" && typeof navigator.onLine === "boolean") {
    return navigator.onLine;
  }
  return true;
};

export const setGlobalSimulatedOffline = (simulateOffline: boolean) => {
  globalSimulatedOffline = simulateOffline;
  listeners.forEach((fn) => fn(simulateOffline));
  if (typeof window !== "undefined") {
    // Dispatch custom event for cross-service reactivity
    window.dispatchEvent(
      new CustomEvent("app-network-change", { detail: { isOnline: getNetworkIsOnline() } })
    );
    if (!simulateOffline) {
      window.dispatchEvent(new Event("online"));
    } else {
      window.dispatchEvent(new Event("offline"));
    }
  }
};

export function useNetworkStatus() {
  const [isOnline, setIsOnline] = useState<boolean>(true);
  const [isSimulatedOffline, setIsSimulated] = useState<boolean>(false);

  useEffect(() => {
    // Initialize with actual state
    setIsOnline(getNetworkIsOnline());
    setIsSimulated(globalSimulatedOffline);

    const updateOnlineStatus = () => {
      setIsOnline(getNetworkIsOnline());
    };

    const handleSimulationChange = (simulated: boolean) => {
      setIsSimulated(simulated);
      setIsOnline(getNetworkIsOnline());
    };

    listeners.push(handleSimulationChange);

    window.addEventListener("online", updateOnlineStatus);
    window.addEventListener("offline", updateOnlineStatus);
    window.addEventListener("app-network-change", updateOnlineStatus);

    return () => {
      window.removeEventListener("online", updateOnlineStatus);
      window.removeEventListener("offline", updateOnlineStatus);
      window.removeEventListener("app-network-change", updateOnlineStatus);
      const idx = listeners.indexOf(handleSimulationChange);
      if (idx !== -1) listeners.splice(idx, 1);
    };
  }, []);

  const toggleSimulation = useCallback(() => {
    setGlobalSimulatedOffline(!globalSimulatedOffline);
  }, []);

  const setSimulation = useCallback((val: boolean) => {
    setGlobalSimulatedOffline(val);
  }, []);

  return {
    isOnline,
    isSimulatedOffline,
    toggleSimulation,
    setSimulation,
  };
}
