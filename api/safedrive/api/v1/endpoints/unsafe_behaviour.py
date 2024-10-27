from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID
from safedrive.database.db import get_db
from safedrive.schemas.unsafe_behaviour import UnsafeBehaviourCreate, UnsafeBehaviourUpdate, UnsafeBehaviourBase as UnsafeBehaviour
from safedrive.crud.unsafe_behaviour import CRUDUnsafeBehaviour as unsafe_behaviour_crud

router = APIRouter()

# Endpoint to create a new unsafe behaviour
@router.post("/unsafe_behaviours/", response_model=UnsafeBehaviour)
def create_unsafe_behaviour(*, db: Session = Depends(get_db), unsafe_behaviour_in: UnsafeBehaviourCreate) -> UnsafeBehaviour:
    try:
        # Validation: Ensure necessary fields are not empty or invalid
        if not unsafe_behaviour_in.tripId or not unsafe_behaviour_in.behaviorType:
            raise HTTPException(status_code=400, detail="Trip ID and behavior type are required")
        return unsafe_behaviour_crud.create(db=db, obj_in=unsafe_behaviour_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error creating unsafe behaviour: {str(e)}")

# Endpoint to get an unsafe behaviour by ID
@router.get("/unsafe_behaviours/{unsafe_behaviour_id}", response_model=UnsafeBehaviour)
def get_unsafe_behaviour(unsafe_behaviour_id: UUID, db: Session = Depends(get_db)) -> UnsafeBehaviour:
    try:
        unsafe_behaviour = unsafe_behaviour_crud.get(db=db, id=unsafe_behaviour_id)
        if not unsafe_behaviour:
            raise HTTPException(status_code=404, detail="Unsafe behaviour not found")
        return unsafe_behaviour
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving unsafe behaviour: {str(e)}")

# Endpoint to get all unsafe behaviours with optional pagination
@router.get("/unsafe_behaviours/", response_model=List[UnsafeBehaviour])
def get_all_unsafe_behaviours(skip: int = 0, limit: int = 20, db: Session = Depends(get_db)) -> List[UnsafeBehaviour]:
    try:
        if limit > 100:
            raise HTTPException(status_code=400, detail="Limit cannot exceed 100 items")
        return unsafe_behaviour_crud.get_all(db=db, skip=skip, limit=limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving unsafe behaviours: {str(e)}")

# Endpoint to update an existing unsafe behaviour
@router.put("/unsafe_behaviours/{unsafe_behaviour_id}", response_model=UnsafeBehaviour)
def update_unsafe_behaviour(unsafe_behaviour_id: UUID, *, db: Session = Depends(get_db), unsafe_behaviour_in: UnsafeBehaviourUpdate) -> UnsafeBehaviour:
    try:
        unsafe_behaviour = unsafe_behaviour_crud.get(db=db, id=unsafe_behaviour_id)
        if not unsafe_behaviour:
            raise HTTPException(status_code=404, detail="Unsafe behaviour not found")
        return unsafe_behaviour_crud.update(db=db, db_obj=unsafe_behaviour, obj_in=unsafe_behaviour_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating unsafe behaviour: {str(e)}")

# Endpoint to delete an unsafe behaviour by ID
@router.delete("/unsafe_behaviours/{unsafe_behaviour_id}", response_model=UnsafeBehaviour)
def delete_unsafe_behaviour(unsafe_behaviour_id: UUID, db: Session = Depends(get_db)) -> UnsafeBehaviour:
    try:
        unsafe_behaviour = unsafe_behaviour_crud.get(db=db, id=unsafe_behaviour_id)
        if not unsafe_behaviour:
            raise HTTPException(status_code=404, detail="Unsafe behaviour not found")
        return unsafe_behaviour_crud.delete(db=db, id=unsafe_behaviour_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting unsafe behaviour: {str(e)}")