from sqlalchemy.orm import Session
from typing import List, Optional
from uuid import UUID
from safedrive.models.driver_profile import DriverProfile
from safedrive.schemas.driver_profile import DriverProfileCreate, DriverProfileUpdate
import logging

logger = logging.getLogger(__name__)

class DriverProfileCRUD:
    """
    CRUD operations for the DriverProfile model.
    """

    def __init__(self, db: Session):
        """
        Initialize the CRUD object with a database session.

        :param db: SQLAlchemy Session object.
        """
        self.db = db

    def create(self, driver_profile_data: DriverProfileCreate) -> DriverProfile:
        """
        Create a new DriverProfile.

        :param driver_profile_data: Data required to create a driver profile.
        :return: The created DriverProfile instance.
        """
        new_profile = DriverProfile(
            email=driver_profile_data.email,
            sync=driver_profile_data.sync,
        )
        try:
            self.db.add(new_profile)
            self.db.commit()
            self.db.refresh(new_profile)
            logger.info(f"Created driver profile with ID: {new_profile.driver_profile_id}")
            return new_profile
        except Exception as e:
            self.db.rollback()
            logger.exception("Error creating driver profile in database.")
            raise

    def get(self, id: UUID) -> Optional[DriverProfile]:
        """
        Retrieve a DriverProfile by ID.

        :param id: UUID of the driver profile.
        :return: DriverProfile instance or None if not found.
        """
        try:
            profile = (
            self.db.query(DriverProfile)
            .filter(DriverProfile.driver_profile_id == str(id))
            .first()
        )
            if profile:
                logger.info(f"Found driver profile with ID: {id}")
            else:
                logger.warning(f"No driver profile found with ID: {id}")
            return profile
        except Exception as e:
            logger.exception("Error retrieving driver profile from database.")
            raise
        
    # Get profile by email
    def get_by_email(self, email: str) -> Optional[DriverProfile]:
        """
        Retrieve a DriverProfile by email.

        :param email: Email of the driver profile.
        :return: DriverProfile instance or None if not found.
        """
        try:
            profile = (
                self.db.query(DriverProfile)
                .filter(DriverProfile.email == email)
                .first()
            )
            if profile:
                logger.info(f"Found driver profile with email: {email}")
            else:
                logger.warning(f"No driver profile found with email: {email}")
            return profile
        except Exception as e:
            logger.exception("Error retrieving driver profile by email from database.")
            raise

    def get_all(self, skip: int = 0, limit: int = 20) -> List[DriverProfile]:
        """
        Retrieve all DriverProfiles with pagination.

        :param skip: Number of records to skip.
        :param limit: Maximum number of records to return.
        :return: List of DriverProfile instances.
        """
        try:
            profiles = self.db.query(DriverProfile).offset(skip).limit(limit).all()
            logger.info(f"Retrieved {len(profiles)} driver profiles from database.")
            return profiles
        except Exception as e:
            logger.exception("Error retrieving driver profiles from database.")
            raise

    def update(self, id: UUID, obj_in: DriverProfileUpdate) -> DriverProfile:
        """
        Update an existing DriverProfile.

        :param id: UUID of the driver profile to update.
        :param obj_in: Data to update.
        :return: Updated DriverProfile instance.
        """
        try:
            db_obj = (
                self.db.query(DriverProfile)
                .filter(DriverProfile.driver_profile_id == str(id))
                .first()
            )
            if not db_obj:
                logger.warning(f"No driver profile found with ID: {id}")
                return None
            if obj_in.email is not None:
                db_obj.email = obj_in.email
            if obj_in.sync is not None:
                db_obj.sync = obj_in.sync
            self.db.commit()
            self.db.refresh(db_obj)
            logger.info(f"Updated driver profile with ID: {id}")
            return db_obj
        except Exception as e:
            self.db.rollback()
            logger.exception("Error updating driver profile in database.")
            raise


    def delete(self, id: UUID) -> Optional[DriverProfile]:
        """
        Delete a DriverProfile by ID.

        :param id: UUID of the driver profile to delete.
        :return: Deleted DriverProfile instance or None if not found.
        """
        try:
            obj = (
                self.db.query(DriverProfile)
                .filter(DriverProfile.driver_profile_id == str(id))
                .first()
            )
            if obj:
                self.db.delete(obj)
                self.db.commit()
                logger.info(f"Deleted driver profile with ID: {id}")
                return obj
            else:
                logger.warning(f"Driver profile with ID {id} not found for deletion.")
                return None
        except Exception as e:
            self.db.rollback()
            logger.exception("Error deleting driver profile from database.")
            raise