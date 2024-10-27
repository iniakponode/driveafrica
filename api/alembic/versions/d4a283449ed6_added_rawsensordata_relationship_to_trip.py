"""added rawsensordata relationship to trip

Revision ID: d4a283449ed6
Revises: dae283ed2ec7
Create Date: 2024-10-14 17:21:59.054697

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'd4a283449ed6'
down_revision: Union[str, None] = 'dae283ed2ec7'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    pass


def downgrade() -> None:
    pass
