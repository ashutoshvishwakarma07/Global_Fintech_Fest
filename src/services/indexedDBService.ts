import { QueuedUploadItem } from "@/types";

const DB_NAME = "ImageUploadDB";
const DB_VERSION = 1;
const STORE_NAME = "uploadQueue";

class IndexedDBService {
  private dbPromise: Promise<IDBDatabase> | null = null;

  private getDB(): Promise<IDBDatabase> {
    if (this.dbPromise) return this.dbPromise;

    this.dbPromise = new Promise((resolve, reject) => {
      if (typeof window === "undefined" || !window.indexedDB) {
        reject(new Error("IndexedDB is not supported in this environment."));
        return;
      }

      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          const store = db.createObjectStore(STORE_NAME, { keyPath: "id" });
          store.createIndex("status", "status", { unique: false });
          store.createIndex("createdAt", "createdAt", { unique: false });
        }
      };

      request.onsuccess = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        resolve(db);
      };

      request.onerror = (event) => {
        console.error("IndexedDB open error:", (event.target as IDBOpenDBRequest).error);
        reject((event.target as IDBOpenDBRequest).error);
      };
    });

    return this.dbPromise;
  }

  /**
   * Save a new capture item into the offline IndexedDB queue
   */
  async saveToQueue(item: QueuedUploadItem): Promise<QueuedUploadItem> {
    const db = await this.getDB();
    return new Promise((resolve, reject) => {
      const transaction = db.transaction([STORE_NAME], "readwrite");
      const store = transaction.objectStore(STORE_NAME);
      const request = store.put(item);

      transaction.oncomplete = () => resolve(item);
      transaction.onerror = (e) => reject((e.target as IDBTransaction).error);
      request.onerror = (e) => reject((e.target as IDBRequest).error);
    });
  }

  /**
   * Retrieve all items currently in the queue
   */
  async getAllQueue(): Promise<QueuedUploadItem[]> {
    const db = await this.getDB();
    return new Promise((resolve, reject) => {
      const transaction = db.transaction([STORE_NAME], "readonly");
      const store = transaction.objectStore(STORE_NAME);
      const request = store.getAll();

      request.onsuccess = () => {
        const items: QueuedUploadItem[] = request.result || [];
        // Sort latest first
        items.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        resolve(items);
      };
      request.onerror = (e) => reject((e.target as IDBRequest).error);
    });
  }

  /**
   * Retrieve only pending or failed items waiting for sync
   */
  async getPendingQueue(): Promise<QueuedUploadItem[]> {
    const all = await this.getAllQueue();
    return all.filter((item) => item.status === "pending" || item.status === "failed");
  }

  /**
   * Retrieve a specific item by ID
   */
  async getQueueItem(id: string): Promise<QueuedUploadItem | null> {
    const db = await this.getDB();
    return new Promise((resolve, reject) => {
      const transaction = db.transaction([STORE_NAME], "readonly");
      const store = transaction.objectStore(STORE_NAME);
      const request = store.get(id);

      request.onsuccess = () => resolve(request.result || null);
      request.onerror = (e) => reject((e.target as IDBRequest).error);
    });
  }

  /**
   * Update item status, retryCount, errorMessage, etc.
   */
  async updateQueueItem(
    id: string,
    updates: Partial<QueuedUploadItem>
  ): Promise<QueuedUploadItem | null> {
    const db = await this.getDB();
    return new Promise((resolve, reject) => {
      const transaction = db.transaction([STORE_NAME], "readwrite");
      const store = transaction.objectStore(STORE_NAME);
      const getReq = store.get(id);

      getReq.onsuccess = () => {
        const existing = getReq.result as QueuedUploadItem | undefined;
        if (!existing) {
          resolve(null);
          return;
        }

        const updated: QueuedUploadItem = { ...existing, ...updates };
        const putReq = store.put(updated);

        transaction.oncomplete = () => resolve(updated);
        putReq.onerror = (e) => reject((e.target as IDBRequest).error);
      };

      getReq.onerror = (e) => reject((e.target as IDBRequest).error);
      transaction.onerror = (e) => reject((e.target as IDBTransaction).error);
    });
  }

  /**
   * Automatically delete an item from IndexedDB once confirmed uploaded
   * Waits for transaction commit to ensure disk persistence
   */
  async removeFromQueue(id: string): Promise<void> {
    const db = await this.getDB();
    return new Promise((resolve, reject) => {
      const transaction = db.transaction([STORE_NAME], "readwrite");
      const store = transaction.objectStore(STORE_NAME);
      const request = store.delete(id);

      transaction.oncomplete = () => {
        console.log(`[IndexedDB] Successfully deleted confirmed record ${id} from uploadQueue`);
        resolve();
      };

      transaction.onerror = (e) => reject((e.target as IDBTransaction).error);
      request.onerror = (e) => reject((e.target as IDBRequest).error);
    });
  }

  /**
   * Verifies that the record is truly removed from IndexedDB
   */
  async verifyDeleted(id: string): Promise<boolean> {
    const item = await this.getQueueItem(id);
    const isDeleted = item === null;
    console.log(`[IndexedDB] Verification for ${id}: ${isDeleted ? "CONFIRMED DELETED" : "STILL PRESENT"}`);
    return isDeleted;
  }

  /**
   * Clear entire queue
   */
  async clearQueue(): Promise<void> {
    const db = await this.getDB();
    return new Promise((resolve, reject) => {
      const transaction = db.transaction([STORE_NAME], "readwrite");
      const store = transaction.objectStore(STORE_NAME);
      const request = store.clear();

      transaction.oncomplete = () => resolve();
      transaction.onerror = (e) => reject((e.target as IDBTransaction).error);
      request.onerror = (e) => reject((e.target as IDBRequest).error);
    });
  }
}

export const indexedDBService = new IndexedDBService();
