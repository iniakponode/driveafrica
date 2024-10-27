from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID
from safedrive.database.db import get_db
from safedrive.schemas.cause import CauseCreate, CauseUpdate, CauseBase as Cause
from safedrive.crud.cause import cause_crud

router = APIRouter()

# Endpoint to create a new cause
@router.post("/causes/", response_model=Cause)
def create_cause(*, db: Session = Depends(get_db), cause_in: CauseCreate) -> Cause:
    try:
        # Validation: Ensure necessary fields are not empty or invalid
        if not cause_in.unsafeBehaviourId or not cause_in.name:
            raise HTTPException(status_code=400, detail="Unsafe Behaviour ID and name are required")
        return cause_crud.create(db=db, obj_in=cause_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error creating cause: {str(e)}")

# Endpoint to get a cause by ID
@router.get("/causes/{cause_id}", response_model=Cause)
def get_cause(cause_id: UUID, db: Session = Depends(get_db)) -> Cause:
    try:
        cause = cause_crud.get(db=db, id=cause_id)
        if not cause:
            raise HTTPException(status_code=404, detail="Cause not found")
        return cause
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving cause: {str(e)}")

# Endpoint to get all causes with optional pagination
@router.get("/causes/", response_model=List[Cause])
def get_all_causes(skip: int = 0, limit: int = 20, db: Session = Depends(get_db)) -> List[Cause]:
    try:
        if limit > 100:
            raise HTTPException(status_code=400, detail="Limit cannot exceed 100 items")
        return cause_crud.get_all(db=db, skip=skip, limit=limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving causes: {str(e)}")

# Endpoint to update an existing cause
@router.put("/causes/{cause_id}", response_model=Cause)
def update_cause(cause_id: UUID, *, db: Session = Depends(get_db), cause_in: CauseUpdate) -> Cause:
    try:
        cause = cause_crud.get(db=db, id=cause_id)
        if not cause:
            raise HTTPException(status_code=404, detail="Cause not found")
        return cause_crud.update(db=db, db_obj=cause, obj_in=cause_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating cause: {str(e)}")

# Endpoint to delete a cause by ID
@router.delete("/causes/{cause_id}", response_model=Cause)
def delete_cause(cause_id: UUID, db: Session = Depends(get_db)) -> Cause:
    try:
        cause = cause_crud.get(db=db, id=cause_id)
        if not cause:
            raise HTTPException(status_code=404, detail="Cause not found")
        return cause_crud.delete(db=db, id=cause_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting cause: {str(e)}")
