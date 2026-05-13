import os
from langchain_community.vectorstores import Chroma
from langchain_openai import OpenAIEmbeddings
from langchain_community.document_loaders import TextLoader, DirectoryLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter

KNOWLEDGE_DIR = "knowledge"
DB_DIR = "chroma_db"

class HRRetriever:
    def __init__(self):
        self.embeddings = None
        self.vector_store = None

    def _get_embeddings(self):
        if not os.getenv("OPENAI_API_KEY"):
            return None
        if self.embeddings is None:
            self.embeddings = OpenAIEmbeddings()
        return self.embeddings
        
    def ingest_documents(self):
        """Load documents from knowledge directory and ingest into ChromaDB."""
        embeddings = self._get_embeddings()
        if embeddings is None:
            return "OPENAI_API_KEY is not configured; knowledge base ingestion is unavailable."

        loader = DirectoryLoader(KNOWLEDGE_DIR, glob="**/*.md", loader_cls=TextLoader)
        documents = loader.load()
        
        text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=100)
        splits = text_splitter.split_documents(documents)
        
        self.vector_store = Chroma.from_documents(
            documents=splits,
            embedding=embeddings,
            persist_directory=DB_DIR
        )
        return f"Ingested {len(splits)} chunks from {len(documents)} documents."

    def get_context(self, query: str, k: int = 3):
        """Retrieve relevant context for a query."""
        embeddings = self._get_embeddings()
        if embeddings is None:
            return "OPENAI_API_KEY is not configured; knowledge base search is unavailable."

        if not self.vector_store:
            # Try to load existing DB
            if os.path.exists(DB_DIR):
                self.vector_store = Chroma(persist_directory=DB_DIR, embedding_function=embeddings)
            else:
                return "Knowledge base not initialized."
                
        results = self.vector_store.similarity_search(query, k=k)
        return "\n---\n".join([doc.page_content for doc in results])

# Singleton instance
hr_retriever = HRRetriever()
