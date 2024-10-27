from sqlalchemy.orm import Session
from uuid import UUID
from datetime import datetime
from typing import List, Optional
from safedrive.models.nlg_report import NLGReport
from safedrive.schemas.nlg_report import NLGReportCreate, NLGReportUpdate


class CRUDNLGReport:
    def __init__(self, model):
        self.model = model

    def create(self, db: Session, obj_in: NLGReportCreate) -> NLGReport:
        """
        Create a new NLG report record in the database.

        Args:
            db (Session): The database session.
            obj_in (NLGReportCreate): The schema with input data for creation.

        Returns:
            NLGReport: The created NLG report.
        """
        db_obj = self.model(**obj_in.dict())
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        return db_obj

    def get(self, db: Session, id: UUID) -> Optional[NLGReport]:
        """
        Retrieve an NLG report record by ID.

        Args:
            db (Session): The database session.
            id (UUID): The ID of the NLG report to retrieve.

        Returns:
            Optional[NLGReport]: The retrieved NLG report or None if not found.
        """
        return db.query(self.model).filter(self.model.id == id).first()

    def get_all(self, db: Session, skip: int = 0, limit: int = 100) -> List[NLGReport]:
        """
        Retrieve all NLG report records from the database.

        Args:
            db (Session): The database session.
            skip (int): Number of records to skip.
            limit (int): Maximum number of records to retrieve.

        Returns:
            List[NLGReport]: A list of NLG reports.
        """
        return db.query(self.model).offset(skip).limit(limit).all()

    def update(self, db: Session, db_obj: NLGReport, obj_in: NLGReportUpdate) -> NLGReport:
        """
        Update an existing NLG report record.

        Args:
            db (Session): The database session.
            db_obj (NLGReport): The existing database object to update.
            obj_in (NLGReportUpdate): The schema with updated data.

        Returns:
            NLGReport: The updated NLG report.
        """
        obj_data = obj_in.dict(exclude_unset=True)
        for field in obj_data:
            setattr(db_obj, field, obj_data[field])
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        return db_obj

    def delete(self, db: Session, id: UUID) -> Optional[NLGReport]:
        """
        Delete an NLG report record by ID.

        Args:
            db (Session): The database session.
            id (UUID): The ID of the NLG report to delete.

        Returns:
            Optional[NLGReport]: The deleted NLG report or None if not found.
        """
        obj = db.query(self.model).filter(self.model.id == id).first()
        if obj:
            db.delete(obj)
            db.commit()
        return obj


# Initialize CRUD instance for NLGReport
nlg_report_crud = CRUDNLGReport(NLGReport)