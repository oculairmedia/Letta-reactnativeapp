import requests
import os


def firecrawl_agent_tool(url: str):
    """
    Firecrawl agent tool to request a website crawl.

    This function sends a request to the self-hosted Firecrawl instance
    to crawl a given URL and return the response.

    Args:
        url (str): The website URL to be crawled.

    Returns:
        str: The response from the Firecrawl API or an error message.
    """
    api_endpoint = os.getenv("FIRECRAWL_API_ENDPOINT")
    if not api_endpoint:
        return "Error: FIRECRAWL_API_ENDPOINT environment variable is not set. Please configure the API endpoint."

    payload = {"url": url}
    headers = {"Content-Type": "application/json"}

    try:
        response = requests.post(
            api_endpoint, json=payload, headers=headers, verify=False
        )
        response.raise_for_status()
        return response.json().get("data", {}).get("markdown", "")
    except requests.exceptions.RequestException as e:
        return f"Crawl request failed: {str(e)}"
