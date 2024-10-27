from pydantic import BaseModel
from typing import Optional
from uuid import UUID
from datetime import datetime

class EmbeddingBase(BaseModel):
    """
    Base class for the Embedding model, defining common attributes.

    Attributes:
    - chunkId (UUID): The unique identifier for each text chunk.
    - chunkText (str): The text content of the chunk.
    - embedding (bytes): The embedding vector represented as a serialized ByteArray.
    - sourceType (str): The type of the source, such as nat_dr_reg_law or ng_high_way_code.
    - sourcePage (Optional[int]): The page number from the source for traceability.
    - createdAt (datetime): The timestamp for when the embedding was created.
    """
    chunkId: UUID
    chunkText: str
    embedding: bytes
    sourceType: str
    sourcePage: Optional[int] = None
    createdAt: datetime

    class Config:
        orm_mode = True

class EmbeddingCreate(EmbeddingBase):
    """
    Schema for creating a new Embedding record.

    Inherits all attributes from EmbeddingBase.
    """
    pass

class EmbeddingUpdate(BaseModel):
    """
    Schema for updating an existing Embedding record.

    Attributes:
    - chunkText (Optional[str]): Optionally update the text content of the chunk.
    - embedding (Optional[bytes]): Optionally update the embedding vector.
    - sourceType (Optional[str]): Optionally update the type of the source.
    - sourcePage (Optional[int]): Optionally update the page number from the source.
    """
    chunkText: Optional[str] = None
    embedding: Optional[bytes] = None
    sourceType: Optional[str] = None
    sourcePage: Optional[int] = None

    class Config:
        orm_mode = True

class EmbeddingResponse(EmbeddingBase):
    """
    Schema for the response format of an Embedding record.

    Inherits all attributes from EmbeddingBase.
    """
    pass