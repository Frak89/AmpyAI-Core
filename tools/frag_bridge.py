"""Local PC bridge. Run adb reverse tcp:8769 tcp:8769 for your paired phone."""
import json
import os
import secrets
import sys
import urllib.request
from http.server import BaseHTTPRequestHandler, HTTPServer

TOKEN = os.environ.get("FRAG_TOKEN") or secrets.token_urlsafe(24)

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/question":
            self.send_error(404)
            return
        if not secrets.compare_digest(self.headers.get("Authorization", ""), "Bearer " + TOKEN):
            self.send_error(403)
            return
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 8192:
                raise ValueError("size")
            question = json.loads(self.rfile.read(size))["question"]
            if not isinstance(question, str) or not 0 < len(question) <= 1200:
                raise ValueError("question")
        except (KeyError, ValueError, TypeError):
            self.send_error(400)
            return
        data = json.dumps({"model": os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
                           "store": False, "max_output_tokens": 600,
                           "instructions": "Rispondi in italiano come collaboratore tecnico di Fråg. Non dichiarare accesso al telefono.",
                           "input": question}).encode()
        req = urllib.request.Request("https://api.openai.com/v1/responses", data=data,
                headers={"Authorization": "Bearer " + os.environ["OPENAI_API_KEY"], "Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=75) as response:
                obj = json.load(response)
            answer = "\n".join(part["text"] for item in obj["output"] for part in item.get("content", [])
                               if part.get("type") == "output_text").strip()
            if not answer:
                raise ValueError("Empty response")
        except (OSError, ValueError, KeyError):
            self.send_error(502)
            return
        body = json.dumps({"answer": answer}).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

if __name__ == "__main__":
    if not os.getenv("OPENAI_API_KEY"):
        sys.exit("Imposta OPENAI_API_KEY sul PC.")
    print("Token Fråg per questo ponte:", TOKEN, flush=True)
    HTTPServer(("127.0.0.1", 8769), Handler).serve_forever()
