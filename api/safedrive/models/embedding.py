from sqlalchemy import Column, String, Integer, LargeBinary
from sqlalchemy.dialects.mysql import BINARY
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.types import TIMESTAMP
from uuid import UUID, uuid4
from safedrive.database.base import Base

def generate_uuid():
        return str(uuid4())  # Generates a UUID string
    
class Embedding(Base):
    
    
    """
    Represents the embeddings stored for different text chunks.

    Attributes:
    - chunk_id (UUID): The unique identifier for the chunk.
    - chunk_text (str): The actual text content of the chunk.
    - embedding (bytes): The embedding representation of the chunk, serialized as binary.
    - source_type (str): The type of source that the chunk is associated with (e.g., regulation or law).
    - source_page (int): The page number in the source where the chunk can be found.
    - created_at (int): A timestamp indicating when the embedding was created.
    """

    
    __tablename__ = "embeddings"

    chunk_id = Column(BINARY(16), primary_key=True, default=generate_uuid, comment="Unique identifier for each chunk of text.")
    chunk_text = Column(String(5000), nullable=False, comment="The text content of the chunk.")
    embedding = Column(LargeBinary, nullable=False, comment="Serialized embedding vector for the chunk.")
    source_type = Column(String(255), nullable=False, comment="The type of source (e.g., nat_dr_reg_law, ng_high_way_code).")
    source_page = Column(Integer, nullable=True, comment="The page number for traceability.")
    created_at = Column(TIMESTAMP, nullable=False, comment="The timestamp indicating when the embedding was created.")

    def __init__(self, chunk_id: UUID, chunk_text: str, embedding: bytes, source_type: str, source_page: int, created_at: int):
        self.chunk_id = chunk_id.bytes
        self.chunk_text = chunk_text
        self.embedding = embedding
        self.source_type = source_type
        self.source_page = source_page
        self.created_at = created_at

    def __repr__(self):
        return f"<Embedding(chunk_id={self.chunk_id}, source_type={self.source_type}, source_page={self.source_page})>"