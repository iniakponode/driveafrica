from sqlalchemy.orm import Session
from uuid import UUID
from datetime import datetime
from safedrive.models.driving_tip import DrivingTip
from safedrive.schemas.driving_tip_sch import DrivingTipCreate, DrivingTipUpdate

class DrivingTipCRUD:
    def __init__(self, db: Session):
        self.db = db

    def get(self, tip_id: UUID) -> DrivingTip:
        """
        Retrieve a DrivingTip entity by its ID.
        
        :param tip_id: UUID of the DrivingTip to retrieve.
        :return: DrivingTip entity if found, else None.
        """
        return self.db.query(DrivingTip).filter(DrivingTip.tipId == tip_id).first()

    def create(self, tip_data: DrivingTipCreate) -> DrivingTip:
        """
        Create a new DrivingTip entity.
        
        :param tip_data: Data required to create a new DrivingTip.
        :return: The newly created DrivingTip entity.
        """
        new_tip = DrivingTip(
            tipId=tip_data.tipId,
            title=tip_data.title,
            meaning=tip_data.meaning,
            penalty=tip_data.penalty,
            fine=tip_data.fine,
            law=tip_data.law,
            hostility=tip_data.hostility,
            summaryTip=tip_data.summaryTip,
            sync=tip_data.sync,
            date=tip_data.date,
            profileId=tip_data.profileId,
            llm=tip_data.llm
        )
        self.db.add(new_tip)
        try:
            self.db.commit()
        except Exception as e:
            self.db.rollback()
            raise e
        self.db.refresh(new_tip)
        return new_tip

    def update(self, tip_id: UUID, tip_data: DrivingTipUpdate) -> DrivingTip:
        """
        Update an existing DrivingTip entity.
        
        :param tip_id: UUID of the DrivingTip to update.
        :param tip_data: Data to update the DrivingTip entity.
        :return: The updated DrivingTip entity if found, else None.
        """
        existing_tip = self.get(tip_id)
        if not existing_tip:
            return None
        
        for key, value in tip_data.dict(exclude_unset=True).items():
            setattr(existing_tip, key, value)
        existing_tip.updatedAt = datetime.utcnow()
        
        try:
            self.db.commit()
        except Exception as e:
            self.db.rollback()
            raise e
        self.db.refresh(existing_tip)
        return existing_tip

    def delete(self, tip_id: UUID) -> bool:
        """
        Delete a DrivingTip entity by its ID.
        
        :param tip_id: UUID of the DrivingTip to delete.
        :return: True if deletion was successful, else False.
        """
        existing_tip = self.get(tip_id)
        if not existing_tip:
            return False
        
        self.db.delete(existing_tip)
        try:
            self.db.commit()
        except Exception as e:
            self.db.rollback()
            raise e
        return True

    def get_all(self, skip: int = 0, limit: int = 10) -> list[DrivingTip]:
        """
        Retrieve a list of DrivingTip entities.
        
        :param skip: Number of entities to skip.
        :param limit: Maximum number of entities to retrieve.
        :return: A list of DrivingTip entities.
        """
        return self.db.query(DrivingTip).offset(skip).limit(limit).all()

# Instantiate an object of DrivingTipCRUD
driving_tip_crud = DrivingTipCRUD(db=Session())