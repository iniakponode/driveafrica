from sqlalchemy.orm import Session
from uuid import UUID
from typing import List, Optional
from safedrive.models.unsafe_behaviour import UnsafeBehaviour
from safedrive.schemas.unsafe_behaviour import UnsafeBehaviourCreate, UnsafeBehaviourUpdate

class CRUDUnsafeBehaviour:
    def __init__(self, model):
        self.model = model

    def create(self, db: Session, obj_in: UnsafeBehaviourCreate) -> UnsafeBehaviour:
        db_obj = self.model(**obj_in.dict())
        db.add(db_obj)
        try:
            db.commit()
        except Exception as e:
            db.rollback()
            raise e
        db.refresh(db_obj)
        return db_obj

    def get(self, db: Session, id: UUID) -> Optional[UnsafeBehaviour]:
        return db.query(self.model).filter(self.model.id == id).first()

    def get_all(self, db: Session, skip: int = 0, limit: int = 100) -> List[UnsafeBehaviour]:
        return db.query(self.model).offset(skip).limit(limit).all()

    def update(self, db: Session, db_obj: UnsafeBehaviour, obj_in: UnsafeBehaviourUpdate) -> UnsafeBehaviour:
        obj_data = obj_in.dict(exclude_unset=True)
        for field in obj_data:
            if hasattr(db_obj, field):
                setattr(db_obj, field, obj_data[field])
        db.add(db_obj)
        try:
            db.commit()
        except Exception as e:
            db.rollback()
            raise e
        db.refresh(db_obj)
        return db_obj

    def delete(self, db: Session, id: UUID) -> Optional[UnsafeBehaviour]:
        obj = db.query(self.model).filter(self.model.id == id).first()
        if obj:
            db.delete(obj)
            try:
                db.commit()
            except Exception as e:
                db.rollback()
                raise e
        return obj

# Initialize CRUD instance for UnsafeBehaviour
unsafe_behaviour_crud = CRUDUnsafeBehaviour(UnsafeBehaviour)