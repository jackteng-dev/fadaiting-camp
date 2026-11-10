import crypto from "node:crypto";

export function isValidLineSignature(signature: string | undefined, body: string, secret: string): boolean {
  if (!signature || !secret) {
    return false;
  }
  const digest = crypto.createHmac("sha256", secret).update(body, "utf8").digest("base64");
  return digest === signature;
}
