from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from uuid import UUID
from safedrive.database.db import get_db
from safedrive.schemas.driver_profile import (
    DriverProfileCreate,
    DriverProfileUpdate,
    DriverProfileResponse,
)
from safedrive.crud.driver_profile import DriverProfileCRUD
import logging

# Set up logging
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)  # Adjust as needed

router = APIRouter()

# Create a new driver profile
@router.post("/driver_profiles/", response_model=DriverProfileResponse)
def create_driver_profile(
    *,
    db: Session = Depends(get_db),
    profile_in: DriverProfileCreate,
) -> DriverProfileResponse:
    """
    Create a new driver profile.

    - **email**: Email address of the driver.
    - **sync**: Optional boolean indicating if the profile has been synced.
    """
    driver_profile_crud = DriverProfileCRUD(db)
    try:
        if not profile_in.email:
            logger.error("Email is required to create a driver profile.")
            raise HTTPException(status_code=400, detail="Email is required")
        new_profile = driver_profile_crud.create(driver_profile_data=profile_in)
        logger.info(f"Driver profile created with ID: {new_profile.driver_profile_id}")
        return DriverProfileResponse.model_validate(new_profile)
    except Exception as e:
        logger.exception("Error creating driver profile")
        raise HTTPException(status_code=500, detail="Internal Server Error")
    
# Get all driver profiles
@router.get("/driver_profiles/", response_model=List[DriverProfileResponse])
def get_all_driver_profiles(
    db: Session = Depends(get_db),
    skip: int = 0,
    limit: int = 10,
) -> List[DriverProfileResponse]:
    """
    Retrieve all driver profiles with pagination.

    - **skip**: Number of profiles to skip.
    - **limit**: Maximum number of profiles to return.
    """
    driver_profile_crud = DriverProfileCRUD(db)
    try:
        profiles = driver_profile_crud.get_all(skip=skip, limit=limit)
        logger.info(f"Retrieved {len(profiles)} driver profiles")
        return [DriverProfileResponse.model_validate(profile) for profile in profiles]
    except Exception as e:
        logger.exception("Error retrieving driver profiles")
        raise HTTPException(status_code=500, detail="Internal Server Error")
    
# Get all driver profile by ID
@router.get("/driver_profiles/{profile_id}", response_model=DriverProfileResponse)
def get_driver_profile(
    profile_id: UUID,
    db: Session = Depends(get_db),
) -> DriverProfileResponse:
    """
    Retrieve a driver profile by ID.

    - **profile_id**: UUID of the driver profile.
    """
    driver_profile_crud = DriverProfileCRUD(db)
    try:
        profile = driver_profile_crud.get(id=profile_id)
        if not profile:
            logger.warning(f"Driver profile with ID {profile_id} not found.")
            raise HTTPException(status_code=404, detail="Driver profile not found")
        logger.info(f"Retrieved driver profile with ID: {profile_id}")
        return DriverProfileResponse.model_validate(profile)
    except HTTPException as e:
        # Re-raise HTTPException so FastAPI can handle it
        raise e
    except Exception as e:
        logger.exception("Error retrieving driver profile")
        raise HTTPException(status_code=500, detail="Internal Server Error")
    
# Get a driver's profile by email
@router.get("/driver_profiles/email/{email}", response_model=DriverProfileResponse)
def get_driver_profile_by_email(
    email: str,
    db: Session = Depends(get_db),
) -> DriverProfileResponse:
    """
    Retrieve a driver profile by email.

    - **email**: Email address of the driver profile.
    """
    driver_profile_crud = DriverProfileCRUD(db)
    try:
        profile = driver_profile_crud.get_by_email(email=email)
        if not profile:
            logger.warning(f"Driver profile with email {email} not found.")
            raise HTTPException(status_code=404, detail="Driver profile not found")
        logger.info(f"Retrieved driver profile with email: {email}")
        return DriverProfileResponse.model_validate(profile)
    except HTTPException as e:
        # Re-raise HTTPException so FastAPI can handle it
        raise e
    except Exception as e:
        logger.exception("Error retrieving driver profile by email")
        raise HTTPException(status_code=500, detail="Internal Server Error")

# Get a driver profile by id
@router.put("/driver_profiles/{profile_id}", response_model=DriverProfileResponse)
def update_driver_profile(
    profile_id: UUID,
    *,
    db: Session = Depends(get_db),
    profile_in: DriverProfileUpdate,
) -> DriverProfileResponse:
    """
    Update an existing driver profile.

    - **profile_id**: UUID of the driver profile to update.
    - **email**: Optional updated email address.
    - **sync**: Optional updated sync status.
    """
    driver_profile_crud = DriverProfileCRUD(db)
    try:
        profile = driver_profile_crud.get(id=profile_id)
        if not profile:
            logger.warning(f"Driver profile with ID {profile_id} not found for update.")
            raise HTTPException(status_code=404, detail="Driver profile not found")
        updated_profile = driver_profile_crud.update(id=profile_id, obj_in=profile_in)
        logger.info(f"Updated driver profile with ID: {profile_id}")
        return DriverProfileResponse.model_validate(updated_profile)
    except HTTPException as e:
        # Re-raise HTTPException so FastAPI can handle it
        raise e
    except Exception as e:
        logger.exception("Error updating driver profile")
        raise HTTPException(status_code=500, detail="Internal Server Error")


# Delete a driver profile by ID
@router.delete("/driver_profiles/{profile_id}", response_model=DriverProfileResponse)
def delete_driver_profile(
    profile_id: UUID,
    db: Session = Depends(get_db),
) -> DriverProfileResponse:
    """
    Delete a driver profile by ID.

    - **profile_id**: UUID of the driver profile to delete.
    """
    driver_profile_crud = DriverProfileCRUD(db)
    try:
        profile = driver_profile_crud.get(id=profile_id)
        if not profile:
            logger.warning(f"Driver profile with ID {profile_id} not found for deletion.")
            raise HTTPException(status_code=404, detail="Driver profile not found")
        deleted_profile = driver_profile_crud.delete(id=profile_id)
        logger.info(f"Deleted driver profile with ID: {profile_id}")
        return DriverProfileResponse.model_validate(deleted_profile)
    except HTTPException as e:
        # Re-raise HTTPException so FastAPI can handle it
        raise e
    except Exception as e:
        logger.exception("Error deleting driver profile")
        raise HTTPException(status_code=500, detail="Internal Server Error")