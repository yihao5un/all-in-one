import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import uvicorn
from langchain_core.messages import HumanMessage
from app.agent.graph import graph

app = FastAPI(title="HR AI Agent Service")

class ChatRequest(BaseModel):
    message: str
    user_id: str = "default"

@app.get("/")
async def root():
    return {"message": "HR AI Agent Service is running"}

@app.post("/chat")
async def chat(request: ChatRequest):
    if not os.getenv("OPENAI_API_KEY"):
        return {
            "response": "AI service is reachable, but OPENAI_API_KEY is not configured. Set it to enable the LangGraph assistant.",
            "status": "degraded"
        }

    try:
        inputs = {"messages": [HumanMessage(content=request.message)]}
        result = await graph.ainvoke(inputs)
        
        # Get the last message from the graph results
        final_message = result["messages"][-1]
        
        return {
            "response": final_message.content,
            "status": "success"
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
