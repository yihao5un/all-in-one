import os
import httpx
from typing import Annotated
from langchain_core.tools import tool
from app.rag.retriever import hr_retriever

GATEWAY_URL = os.getenv("BACKEND_GATEWAY_URL", "http://localhost:8088")

@tool
async def query_order_status(order_no: Annotated[str, "The unique order number to check status for"]):
    """Query the status and details of a specific HR order."""
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(f"{GATEWAY_URL}/order/{order_no}")
            if response.status_code == 200:
                return response.json()
            return f"Error: Received status code {response.status_code} from order service."
        except Exception as e:
            return f"Error connecting to order service: {str(e)}"

@tool
async def list_recent_orders(limit: Annotated[int, "Number of recent orders to fetch"] = 5):
    """Fetch a list of recent HR orders (onboarding, transfer, resignation)."""
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(f"{GATEWAY_URL}/order/list?limit={limit}")
            if response.status_code == 200:
                return response.json()
            return f"Error: Received status code {response.status_code} from order service."
        except Exception as e:
            return f"Error connecting to order service: {str(e)}"

@tool
async def check_employee_eligibility(employee_id: Annotated[str, "The ID of the employee to check"]):
    """Check if an employee is eligible for transfer or specific benefits."""
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(f"{GATEWAY_URL}/product/check-eligibility/{employee_id}")
            if response.status_code == 200:
                return response.json()
            return f"Error checking eligibility: {response.text}"
        except Exception as e:
            return f"Error connecting to product service: {str(e)}"

@tool
async def search_hr_knowledge(query: Annotated[str, "The search query for HR policies and documentation"]):
    """Search the HR knowledge base for policies, procedures, and company documentation."""
    try:
        context = hr_retriever.get_context(query)
        return context
    except Exception as e:
        return f"Error searching knowledge base: {str(e)}"
