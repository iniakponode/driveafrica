from sqlalchemy.orm import Session
from uuid import UUID
from typing import List, Optional
from safedrive.models.location import Location
from safedrive.schemas.location import LocationCreate, LocationUpdate


class CRUDLocation:
    def __init__(self, model):
        self.model = model

    def create(self, db: Session, obj_in: LocationCreate) -> Location:
        """
        Create a new Location record in the database.

        Args:
            db (Session): The database session.
            obj_in (LocationCreate): The schema with input data for creation.

        Returns:
            Location: The created Location.
        """
        db_obj = self.model(**obj_in.dict())
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        return db_obj

    def get(self, db: Session, id: UUID) -> Optional[Location]:
        """
        Retrieve a Location record by ID.

        Args:
            db (Session): The database session.
            id (UUID): The ID of the Location to retrieve.

        Returns:
            Optional[Location]: The retrieved Location or None if not found.
        """
        return db.query(self.model).filter(self.model.id == id).first()

    def get_all(self, db: Session, skip: int = 0, limit: int = 100) -> List[Location]:
        """
        Retrieve all Location records from the database.

        Args:
            db (Session): The database session.
            skip (int): Number of records to skip.
            limit (int): Maximum number of records to retrieve.

        Returns:
            List[Location]: A list of Locations.
        """
        return db.query(self.model).offset(skip).limit(limit).all()

    def update(self, db: Session, db_obj: Location, obj_in: LocationUpdate) -> Location:
        """
        Update an existing Location record.

        Args:
            db (Session): The database session.
            db_obj (Location): The existing database object to update.
            obj_in (LocationUpdate): The schema with updated data.

        Returns:
            Location: The updated Location.
        """
        obj_data = obj_in.dict(exclude_unset=True)
        for field in obj_data:
            setattr(db_obj, field, obj_data[field])
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        return db_obj

    def delete(self, db: Session, id: UUID) -> Optional[Location]:
        """
        Delete a Location record by ID.

        Args:
            db (Session): The database session.
            id (UUID): The ID of the Location to delete.

        Returns:
            Optional[Location]: The deleted Location or None if not found.
        """
        obj = db.query(self.model).filter(self.model.id == id).first()
        if obj:
            db.delete(obj)
            db.commit()
        return obj


# Initialize CRUD instance for Location
location_crud = CRUDLocation(Location)