from sqlalchemy.orm import Session
from uuid import UUID
from typing import List, Optional
import logging

from safedrive.models.raw_sensor_data import RawSensorData
from safedrive.schemas.raw_sensor_data import RawSensorDataCreate, RawSensorDataUpdate

logger = logging.getLogger(__name__)

class CRUDRawSensorData:
    """
    CRUD operations for the RawSensorData model.
    """

    def __init__(self, model):
        """
        Initialize the CRUD object with a database model.

        :param model: The SQLAlchemy model class.
        """
        self.model = model

    def create(self, db: Session, obj_in: RawSensorDataCreate) -> RawSensorData:
        """
        Create a new raw sensor data record in the database.

        :param db: The database session.
        :param obj_in: The schema with input data for creation.
        :return: The created raw sensor data.
        """
        try:
            obj_data = obj_in.dict()
            # Convert UUID fields to bytes
            if 'location_id' in obj_data and isinstance(obj_data['location_id'], UUID):
                obj_data['location_id'] = obj_data['location_id'].bytes
            if 'trip_id' in obj_data and isinstance(obj_data['trip_id'], UUID):
                obj_data['trip_id'] = obj_data['trip_id'].bytes
            db_obj = self.model(**obj_data)
            db.add(db_obj)
            db.commit()
            db.refresh(db_obj)
            logger.info(f"Created raw sensor data with ID: {db_obj.id.hex()}")
            return db_obj
        except Exception as e:
            db.rollback()
            logger.exception("Error creating raw sensor data in database.")
            raise

    def get(self, db: Session, id: UUID) -> Optional[RawSensorData]:
        """
        Retrieve a raw sensor data record by ID.

        :param db: The database session.
        :param id: The UUID of the raw sensor data to retrieve.
        :return: The retrieved raw sensor data or None if not found.
        """
        try:
            data = db.query(self.model).filter(self.model.id == id.bytes).first()
            if data:
                logger.info(f"Found raw sensor data with ID: {id}")
            else:
                logger.warning(f"No raw sensor data found with ID: {id}")
            return data
        except Exception as e:
            logger.exception("Error retrieving raw sensor data from database.")
            raise

    def get_all(self, db: Session, skip: int = 0, limit: int = 100) -> List[RawSensorData]:
        """
        Retrieve all raw sensor data records from the database.

        :param db: The database session.
        :param skip: Number of records to skip.
        :param limit: Maximum number of records to retrieve.
        :return: A list of raw sensor data records.
        """
        try:
            data_list = db.query(self.model).offset(skip).limit(limit).all()
            logger.info(f"Retrieved {len(data_list)} raw sensor data records from database.")
            return data_list
        except Exception as e:
            logger.exception("Error retrieving raw sensor data from database.")
            raise

    def update(self, db: Session, db_obj: RawSensorData, obj_in: RawSensorDataUpdate) -> RawSensorData:
        """
        Update an existing raw sensor data record.

        :param db: The database session.
        :param db_obj: The existing database object to update.
        :param obj_in: The schema with updated data.
        :return: The updated raw sensor data.
        """
        try:
            obj_data = obj_in.dict(exclude_unset=True)
            for field in obj_data:
                if field in ['location_id', 'trip_id'] and isinstance(obj_data[field], UUID):
                    setattr(db_obj, field, obj_data[field].bytes)
                else:
                    setattr(db_obj, field, obj_data[field])
            db.commit()
            db.refresh(db_obj)
            logger.info(f"Updated raw sensor data with ID: {db_obj.id.hex()}")
            return db_obj
        except Exception as e:
            db.rollback()
            logger.exception("Error updating raw sensor data in database.")
            raise

    def delete(self, db: Session, id: UUID) -> Optional[RawSensorData]:
        """
        Delete a raw sensor data record by ID.

        :param db: The database session.
        :param id: The UUID of the raw sensor data to delete.
        :return: The deleted raw sensor data or None if not found.
        """
        try:
            obj = db.query(self.model).filter(self.model.id == id.bytes).first()
            if obj:
                db.delete(obj)
                db.commit()
                logger.info(f"Deleted raw sensor data with ID: {id}")
                return obj
            else:
                logger.warning(f"Raw sensor data with ID {id} not found for deletion.")
                return None
        except Exception as e:
            db.rollback()
            logger.exception("Error deleting raw sensor data from database.")
            raise

# Initialize CRUD instance for RawSensorData
raw_sensor_data_crud = CRUDRawSensorData(RawSensorData)