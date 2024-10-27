from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID
from safedrive.database.db import get_db
from safedrive.schemas.driving_tip_sch import DrivingTipCreate, DrivingTipUpdate, DrivingTipBase as DrivingTip
from safedrive.crud.driving_tip import driving_tip_crud

router = APIRouter()

# Endpoint to create a new driving tip
@router.post("/driving-tips/", response_model=DrivingTip)
def create_driving_tip(*, db: Session = Depends(get_db), tip_in: DrivingTipCreate) -> DrivingTip:
    try:
        if not tip_in.title or not tip_in.profileId:
            raise HTTPException(status_code=400, detail="Title and Profile ID are required")
        return driving_tip_crud.create(db=db, obj_in=tip_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error creating driving tip: {str(e)}")

# Endpoint to get a driving tip by ID
@router.get("/driving-tips/{tip_id}", response_model=DrivingTip)
def get_driving_tip(tip_id: UUID, db: Session = Depends(get_db)) -> DrivingTip:
    try:
        tip = driving_tip_crud.get(db=db, id=tip_id)
        if not tip:
            raise HTTPException(status_code=404, detail="Driving tip not found")
        return tip
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving driving tip: {str(e)}")

# Endpoint to get all driving tips with optional pagination
@router.get("/driving-tips/", response_model=List[DrivingTip])
def get_all_driving_tips(skip: int = 0, limit: int = 20, db: Session = Depends(get_db)) -> List[DrivingTip]:
    try:
        if limit > 100:
            raise HTTPException(status_code=400, detail="Limit cannot exceed 100 items")
        return driving_tip_crud.get_all(db=db, skip=skip, limit=limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving driving tips: {str(e)}")

# Endpoint to update an existing driving tip
@router.put("/driving-tips/{tip_id}", response_model=DrivingTip)
def update_driving_tip(tip_id: UUID, *, db: Session = Depends(get_db), tip_in: DrivingTipUpdate) -> DrivingTip:
    try:
        tip = driving_tip_crud.get(db=db, id=tip_id)
        if not tip:
            raise HTTPException(status_code=404, detail="Driving tip not found")
        return driving_tip_crud.update(db=db, db_obj=tip, obj_in=tip_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating driving tip: {str(e)}")

# Endpoint to delete a driving tip by ID
@router.delete("/driving-tips/{tip_id}", response_model=DrivingTip)
def delete_driving_tip(tip_id: UUID, db: Session = Depends(get_db)) -> DrivingTip:
    try:
        tip = driving_tip_crud.get(db=db, id=tip_id)
        if not tip:
            raise HTTPException(status_code=404, detail="Driving tip not found")
        return driving_tip_crud.delete(db=db, id=tip_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting driving tip: {str(e)}")