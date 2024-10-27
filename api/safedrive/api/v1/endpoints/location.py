from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID
from safedrive.database.db import get_db
from safedrive.schemas.location import LocationCreate, LocationUpdate, LocationBase as Location
from safedrive.crud.location import location_crud

router = APIRouter()

# Endpoint to create a new location
@router.post("/locations/", response_model=Location)
def create_location(*, db: Session = Depends(get_db), location_in: LocationCreate) -> Location:
    try:
        # Validation: Ensure necessary fields are not empty or invalid
        if not location_in.latitude or not location_in.longitude:
            raise HTTPException(status_code=400, detail="Latitude and Longitude are required")
        return location_crud.create(db=db, obj_in=location_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error creating location: {str(e)}")

# Endpoint to get a location by ID
@router.get("/locations/{location_id}", response_model=Location)
def get_location(location_id: UUID, db: Session = Depends(get_db)) -> Location:
    try:
        location = location_crud.get(db=db, id=location_id)
        if not location:
            raise HTTPException(status_code=404, detail="Location not found")
        return location
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving location: {str(e)}")

# Endpoint to get all locations with optional pagination
@router.get("/locations/", response_model=List[Location])
def get_all_locations(skip: int = 0, limit: int = 20, db: Session = Depends(get_db)) -> List[Location]:
    try:
        if limit > 100:
            raise HTTPException(status_code=400, detail="Limit cannot exceed 100 items")
        return location_crud.get_all(db=db, skip=skip, limit=limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving locations: {str(e)}")

# Endpoint to update an existing location
@router.put("/locations/{location_id}", response_model=Location)
def update_location(location_id: UUID, *, db: Session = Depends(get_db), location_in: LocationUpdate) -> Location:
    try:
        location = location_crud.get(db=db, id=location_id)
        if not location:
            raise HTTPException(status_code=404, detail="Location not found")
        return location_crud.update(db=db, db_obj=location, obj_in=location_in)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error updating location: {str(e)}")

# Endpoint to delete a location by ID
@router.delete("/locations/{location_id}", response_model=Location)
def delete_location(location_id: UUID, db: Session = Depends(get_db)) -> Location:
    try:
        location = location_crud.get(db=db, id=location_id)
        if not location:
            raise HTTPException(status_code=404, detail="Location not found")
        return location_crud.delete(db=db, id=location_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting location: {str(e)}")