from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID
from safedrive.database.db import get_db
from safedrive.schemas.embedding import EmbeddingCreate, EmbeddingUpdate, EmbeddingBase as Embedding
from safedrive.crud.embedding import embedding_crud

router = APIRouter()

# Endpoint to create a new embedding
@router.post("/embeddings/", response_model=Embedding)
def create_embedding(*, db: Session = Depends(get_db), embedding_in: EmbeddingCreate) -> Embedding:
    try:
        # Validation: Ensure necessary fields are not empty or invalid
        if not embedding_in.chunkText or not embedding_in.sourceType:
            raise HTTPException(status_code=400, detail="Chunk text and source type are required")
        return embedding_crud.create(db=db, obj_in=embedding_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error creating embedding: {str(e)}")

# Endpoint to get an embedding by ID
@router.get("/embeddings/{embedding_id}", response_model=Embedding)
def get_embedding(embedding_id: UUID, db: Session = Depends(get_db)) -> Embedding:
    try:
        embedding = embedding_crud.get(db=db, id=embedding_id)
        if not embedding:
            raise HTTPException(status_code=404, detail="Embedding not found")
        return embedding
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving embedding: {str(e)}")

# Endpoint to get all embeddings with optional pagination
@router.get("/embeddings/", response_model=List[Embedding])
def get_all_embeddings(skip: int = 0, limit: int = 20, db: Session = Depends(get_db)) -> List[Embedding]:
    try:
        if limit > 100:
            raise HTTPException(status_code=400, detail="Limit cannot exceed 100 items")
        return embedding_crud.get_all(db=db, skip=skip, limit=limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving embeddings: {str(e)}")

# Endpoint to update an existing embedding
@router.put("/embeddings/{embedding_id}", response_model=Embedding)
def update_embedding(embedding_id: UUID, *, db: Session = Depends(get_db), embedding_in: EmbeddingUpdate) -> Embedding:
    try:
        embedding = embedding_crud.get(db=db, id=embedding_id)
        if not embedding:
            raise HTTPException(status_code=404, detail="Embedding not found")
        return embedding_crud.update(db=db, db_obj=embedding, obj_in=embedding_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating embedding: {str(e)}")

# Endpoint to delete an embedding by ID
@router.delete("/embeddings/{embedding_id}", response_model=Embedding)
def delete_embedding(embedding_id: UUID, db: Session = Depends(get_db)) -> Embedding:
    try:
        embedding = embedding_crud.get(db=db, id=embedding_id)
        if not embedding:
            raise HTTPException(status_code=404, detail="Embedding not found")
        return embedding_crud.delete(db=db, id=embedding_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting embedding: {str(e)}")